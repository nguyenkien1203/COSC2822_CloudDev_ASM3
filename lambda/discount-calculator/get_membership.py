import json
import boto3
import urllib.request
import os

# Profile service URL (ALB)
PROFILE_SERVICE_URL = os.environ.get('PROFILE_SERVICE_URL', 'http://restaurant-alb-696207792.us-east-1.elb.amazonaws.com')

def handler(event, context):
    """
    Lambda function to get membership info from profile-service
    
    Input: { "userId": "cognito-sub-uuid" }
    Output: { "membershipRank": "GOLD", "discountPercentage": 10, "loyaltyPoints": 500 }
    """
    try:
        user_id = event.get('userId')
        
        if not user_id:
            print("No userId provided, returning default membership")
            return {
                'membershipRank': 'SILVER',
                'discountPercentage': 5,
                'loyaltyPoints': 0
            }
        
        # Call profile-service to get membership info
        url = f"{PROFILE_SERVICE_URL}/api/profiles/user/{user_id}/membership"
        print(f"Calling profile service: {url}")
        
        req = urllib.request.Request(url, method='GET')
        req.add_header('Content-Type', 'application/json')
        
        with urllib.request.urlopen(req, timeout=10) as response:
            data = json.loads(response.read().decode())
            print(f"Profile service response: {data}")
            
            return {
                'membershipRank': data.get('membershipRank', 'SILVER'),
                'discountPercentage': data.get('discountPercentage', 5),
                'loyaltyPoints': data.get('loyaltyPoints', 0)
            }
            
    except urllib.error.HTTPError as e:
        print(f"HTTP Error: {e.code} - {e.reason}")
        # Return default for non-existent users
        return {
            'membershipRank': 'SILVER',
            'discountPercentage': 5,
            'loyaltyPoints': 0
        }
        
    except Exception as e:
        print(f"Error getting membership info: {str(e)}")
        # Return default on any error
        return {
            'membershipRank': 'SILVER', 
            'discountPercentage': 5,
            'loyaltyPoints': 0
        }
