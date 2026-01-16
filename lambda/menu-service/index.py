import json
import boto3
import uuid
import base64
import urllib.request
import os
from datetime import datetime
from decimal import Decimal
from boto3.dynamodb.conditions import Key, Attr

# Initialize DynamoDB
dynamodb = boto3.resource('dynamodb')
TABLE_NAME = 'restaurant-menu'
table = dynamodb.Table(TABLE_NAME)

# Initialize S3
s3_client = boto3.client('s3')
S3_BUCKET_NAME = os.environ.get('S3_BUCKET_NAME', 'restaurant-menu-images-616967801866')
S3_REGION = os.environ.get('S3_REGION', 'us-east-1')

# Cognito configuration
COGNITO_REGION = 'us-east-1'
USER_POOL_ID = 'us-east-1_YyEqs4kkV'  
COOKIE_NAME = 'auth_token'

# Cache for JWKS
_jwks_cache = None


def handler(event, context):
    """Main Lambda handler - routes requests to appropriate function"""
    try:
        http_method = event['requestContext']['http']['method']
        path = event['rawPath']
        
        print(f"Received: {http_method} {path}")
        
        # Handle OPTIONS for CORS preflight - return 200 with CORS headers
        if http_method == 'OPTIONS':
            return {
                'statusCode': 200,
                'headers': get_cors_headers(event),
                'body': ''
            }
        
        # Route based on path and method
        if path == '/api/menu/available' and http_method == 'GET':
            return get_available_menus(event)
        elif path == '/api/menu' and http_method == 'GET':
            return get_all_menus(event)
        elif path == '/api/menu/all' and http_method == 'GET':
            return get_all_menus_public(event)
        elif path == '/api/menu' and http_method == 'POST':
            return create_menu(event)
        elif path == '/api/menu/upload-url' and http_method == 'POST':
            return get_upload_url(event)
        elif path.startswith('/api/menu/') and path.endswith('/toggle-availability') and http_method == 'PATCH':
            menu_id = path.split('/')[3]
            return toggle_availability(menu_id, event)
        elif path.startswith('/api/menu/') and http_method == 'GET':
            menu_id = path.split('/')[3]
            return get_menu_by_id(menu_id, event)
        elif path.startswith('/api/menu/') and http_method == 'PUT':
            menu_id = path.split('/')[3]
            return update_menu(menu_id, event)
        elif path.startswith('/api/menu/') and http_method == 'DELETE':
            menu_id = path.split('/')[3]
            return delete_menu(menu_id, event)
        else:
            return response(404, {'error': 'Not Found'}, event)
    
    except Exception as e:
        print(f"Error: {str(e)}")
        import traceback
        traceback.print_exc()
        return response(500, {'error': str(e)}, event)


# ============== PUBLIC ENDPOINTS ==============

def get_available_menus(event):
    """GET /api/menu/available - Public endpoint"""
    params = event.get('queryStringParameters') or {}
    category = params.get('category')
    
    if category:
        result = table.query(
            IndexName='category-availability-index',
            KeyConditionExpression=Key('category').eq(category) & Key('isAvailable').eq('true')
        )
    else:
        result = table.scan(
            FilterExpression=Attr('isAvailable').eq('true')
        )
    
    items = result.get('Items', [])
    return response(200, convert_decimals(items), event)

def get_all_menus_public(event):
    """GET /api/menu/all - Public endpoint"""
    params = event.get('queryStringParameters') or {}
    category = params.get('category')
    
    if category:
        result = table.query(
            IndexName='category-availability-index',
            KeyConditionExpression=Key('category').eq(category)
        )
    else:
        result = table.scan()
    
    items = result.get('Items', [])
    return response(200, convert_decimals(items), event)
   

def get_menu_by_id(menu_id, event):
    """GET /api/menu/{id} - Public endpoint"""
    result = table.get_item(Key={'id': menu_id})
    
    if 'Item' not in result:
        return response(404, {'error': 'Menu item not found'}, event)
    
    return response(200, convert_decimals(result['Item']), event)


# ============== ADMIN ENDPOINTS ==============

def get_all_menus(event):
    """GET /api/menu - Admin only"""
    auth_result = authenticate_admin(event)
    if auth_result['error']:
        return auth_result['response']
    
    result = table.scan()
    items = result.get('Items', [])
    return response(200, convert_decimals(items), event)


def get_upload_url(event):
    """POST /api/menu/upload-url - Admin only
    Returns a presigned URL for uploading images to S3
    """
    auth_result = authenticate_admin(event)
    if auth_result['error']:
        return auth_result['response']
    
    # Handle empty or None body
    body_str = event.get('body') or '{}'
    try:
        body = json.loads(body_str)
    except json.JSONDecodeError:
        return response(400, {'error': 'Invalid JSON body. Expected: {"filename": "image.jpg", "contentType": "image/jpeg"}'}, event)
    
    filename = body.get('filename', 'image.jpg')
    content_type = body.get('contentType', 'image/jpeg')
    
    # Generate unique key
    ext = filename.split('.')[-1] if '.' in filename else 'jpg'
    key = f"menu-images/{uuid.uuid4()}.{ext}"
    
    # Generate presigned URL for PUT (upload)
    presigned_url = s3_client.generate_presigned_url(
        'put_object',
        Params={
            'Bucket': S3_BUCKET_NAME,
            'Key': key,
            'ContentType': content_type
        },
        ExpiresIn=300  # 5 minutes
    )
    
    # The public URL where the image will be accessible
    image_url = f"https://{S3_BUCKET_NAME}.s3.{S3_REGION}.amazonaws.com/{key}"
    
    return response(200, {
        'uploadUrl': presigned_url,
        'imageUrl': image_url,
        'key': key
    }, event)


def create_menu(event):
    """POST /api/menu - Admin only"""
    auth_result = authenticate_admin(event)
    if auth_result['error']:
        return auth_result['response']
    
    body = json.loads(event.get('body', '{}'))
    now = datetime.utcnow().isoformat()
    
    # Process tags: ensure it is a list
    tags = body.get('tags', [])
    if not isinstance(tags, list):
        tags = []

    # Convert isAvailable to string for DynamoDB GSI (expects 'true' or 'false')
    is_available = body.get('isAvailable', True)
    if isinstance(is_available, bool):
        is_available = 'true' if is_available else 'false'
    elif isinstance(is_available, str):
        is_available = 'true' if is_available.lower() == 'true' else 'false'
    else:
        is_available = 'true'

    item = {
        'id': str(uuid.uuid4()),
        'name': body.get('name'),
        'description': body.get('description', ''),
        'price': Decimal(str(body.get('price', 0))),
        'category': body.get('category', ''),
        'imageUrl': body.get('imageUrl', ''),
        'tags': tags,
        'isAvailable': is_available,
        'preparationTime': body.get('preparationTime', 0),
        'calories': body.get('calories', 0),
        'createdAt': now,
        'updatedAt': now
    }
    
    table.put_item(Item=item)
    return response(201, convert_decimals(item), event)


def update_menu(menu_id, event):
    """PUT /api/menu/{id} - Admin only"""
    auth_result = authenticate_admin(event)
    if auth_result['error']:
        return auth_result['response']
    
    existing = table.get_item(Key={'id': menu_id})
    if 'Item' not in existing:
        return response(404, {'error': 'Menu item not found'}, event)
    
    body = json.loads(event.get('body', '{}'))
    now = datetime.utcnow().isoformat()
    
    update_expr = "SET updatedAt = :updatedAt"
    expr_values = {':updatedAt': now}
    expr_names = {}
    
    if 'name' in body:
        update_expr += ", #n = :name"
        expr_values[':name'] = body['name']
        expr_names['#n'] = 'name'
    if 'description' in body:
        update_expr += ", description = :desc"
        expr_values[':desc'] = body['description']
    if 'price' in body:
        update_expr += ", price = :price"
        expr_values[':price'] = Decimal(str(body['price']))
    if 'category' in body:
        update_expr += ", category = :cat"
        expr_values[':cat'] = body['category']
    if 'imageUrl' in body:
        update_expr += ", imageUrl = :img"
        expr_values[':img'] = body['imageUrl']
    if 'preparationTime' in body:
        update_expr += ", preparationTime = :prep"
        expr_values[':prep'] = body['preparationTime']
    if 'calories' in body:
        update_expr += ", calories = :cal"
        expr_values[':cal'] = body['calories']
    
    # Handle tags update
    if 'tags' in body:
        update_expr += ", tags = :tags"
        tags_input = body['tags']
        expr_values[':tags'] = tags_input if isinstance(tags_input, list) else []
    
    update_params = {
        'Key': {'id': menu_id},
        'UpdateExpression': update_expr,
        'ExpressionAttributeValues': expr_values,
        'ReturnValues': 'ALL_NEW'
    }
    if expr_names:
        update_params['ExpressionAttributeNames'] = expr_names
    
    result = table.update_item(**update_params)
    return response(200, convert_decimals(result['Attributes']), event)


def delete_menu(menu_id, event):
    """DELETE /api/menu/{id} - Admin only"""
    auth_result = authenticate_admin(event)
    if auth_result['error']:
        return auth_result['response']
    
    table.delete_item(Key={'id': menu_id})
    return response(204, None, event)


def toggle_availability(menu_id, event):
    """PATCH /api/menu/{id}/toggle-availability - Admin only"""
    auth_result = authenticate_admin(event)
    if auth_result['error']:
        return auth_result['response']
    
    existing = table.get_item(Key={'id': menu_id})
    if 'Item' not in existing:
        return response(404, {'error': 'Menu item not found'}, event)
    
    current = existing['Item']['isAvailable']
    new_value = 'false' if current == 'true' else 'true'
    
    result = table.update_item(
        Key={'id': menu_id},
        UpdateExpression="SET isAvailable = :val, updatedAt = :now",
        ExpressionAttributeValues={
            ':val': new_value,
            ':now': datetime.utcnow().isoformat()
        },
        ReturnValues='ALL_NEW'
    )
    
    return response(200, convert_decimals(result['Attributes']), event)


# ============== AUTHENTICATION ==============

def authenticate_admin(event):
    """Authenticate user from cookie and check ADMIN role"""
    # Get JWT from cookie
    token = get_token_from_cookie(event)
    
    if not token:
        return {
            'error': True,
            'response': response(401, {'error': 'Unauthorized', 'message': 'No authentication token found'}, event)
        }
    
    # Decode and validate JWT
    try:
        claims = decode_jwt(token)
        
        if not claims:
            return {
                'error': True,
                'response': response(401, {'error': 'Unauthorized', 'message': 'Invalid or expired token'}, event)
            }
        
        # Check for ADMIN group
        groups = claims.get('cognito:groups', [])
        if isinstance(groups, str):
            groups = [groups]
        
        # print(f"User groups: {groups}")
        
        if 'ADMIN' not in groups:
            return {
                'error': True,
                'response': response(403, {'error': 'Forbidden', 'message': 'Admin access required'}, event)
            }
        
        return {'error': False, 'claims': claims}
        
    except Exception as e:
        print(f"Authentication error: {e}")
        import traceback
        traceback.print_exc()
        return {
            'error': True,
            'response': response(401, {'error': 'Unauthorized', 'message': str(e)}, event)
        }


def get_token_from_cookie(event):
    """Extract JWT token from cookies"""
    cookies = event.get('cookies', [])
    
    # Try header format first (API Gateway HTTP API v2)
    if cookies:
        for cookie in cookies:
            if cookie.startswith(f'{COOKIE_NAME}='):
                return cookie.split('=', 1)[1]
    
    # Try headers (API Gateway REST API or different format)
    headers = event.get('headers', {})
    cookie_header = headers.get('cookie') or headers.get('Cookie', '')
    
    if cookie_header:
        for part in cookie_header.split(';'):
            part = part.strip()
            if part.startswith(f'{COOKIE_NAME}='):
                return part.split('=', 1)[1]
    
    # Also check Authorization header as fallback
    auth_header = headers.get('authorization') or headers.get('Authorization', '')
    if auth_header.startswith('Bearer '):
        return auth_header[7:]
    
    return None


def decode_jwt(token):
    """Decode JWT without full signature verification (for demo purposes)
    In production, use python-jose or similar library with JWKS verification
    """
    try:
        # Split JWT
        parts = token.split('.')
        if len(parts) != 3:
            return None
        
        # Decode payload (middle part)
        payload = parts[1]
        # Add padding if needed
        padding = 4 - len(payload) % 4
        if padding != 4:
            payload += '=' * padding
        
        decoded = base64.urlsafe_b64decode(payload)
        claims = json.loads(decoded)
        
        # Check expiration
        exp = claims.get('exp', 0)
        now = datetime.utcnow().timestamp()
        
        if exp < now:
            print(f"Token expired: exp={exp}, now={now}")
            return None
        
        return claims
        
    except Exception as e:
        print(f"JWT decode error: {e}")
        return None


# ============== UTILITIES ==============

def get_cors_headers(event=None):
    """Get CORS headers with dynamic origin support"""
    origin = 'http://localhost:3000'
    
    if event:
        headers = event.get('headers', {})
        request_origin = headers.get('origin') or headers.get('Origin', '')
        # Allow localhost origins for development
        if request_origin and ('localhost' in request_origin or '127.0.0.1' in request_origin):
            origin = request_origin
    
    return {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': origin,
        'Access-Control-Allow-Headers': 'Content-Type, Authorization, Cookie',
        'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, PATCH, OPTIONS',
        'Access-Control-Allow-Credentials': 'true'
    }


def response(status_code, body, event=None):
    """Create API Gateway response with CORS headers"""
    return {
        'statusCode': status_code,
        'headers': get_cors_headers(event),
        'body': json.dumps(body) if body else ''
    }


def convert_decimals(obj):
    """Convert Decimal types to float for JSON serialization"""
    if isinstance(obj, list):
        return [convert_decimals(i) for i in obj]
    elif isinstance(obj, dict):
        return {k: convert_decimals(v) for k, v in obj.items()}
    elif isinstance(obj, Decimal):
        return float(obj)
    else:
        return obj
