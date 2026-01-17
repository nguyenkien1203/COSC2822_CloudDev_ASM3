import json
from decimal import Decimal

def handler(event, context):
    """
    Lambda function to calculate discount based on membership info and subtotal
    
    Input: {
        "userId": "cognito-sub-uuid",
        "subtotal": 100.00,
        "membershipRank": "GOLD",
        "discountPercentage": 10,
        "loyaltyPoints": 500
    }
    
    Output: {
        "userId": "cognito-sub-uuid",
        "subtotal": 100.00,
        "membershipRank": "GOLD",
        "discountPercentage": 10,
        "discountAmount": 10.00,
        "loyaltyPoints": 500
    }
    """
    try:
        user_id = event.get('userId')
        subtotal = Decimal(str(event.get('subtotal', 0)))
        membership_rank = event.get('membershipRank', 'SILVER')
        discount_percentage = event.get('discountPercentage', 5)
        loyalty_points = event.get('loyaltyPoints', 0)
        
        print(f"Calculating discount for userId: {user_id}, subtotal: {subtotal}, rank: {membership_rank}, percentage: {discount_percentage}%")
        
        # Calculate discount amount
        discount_amount = (subtotal * Decimal(discount_percentage) / Decimal(100)).quantize(Decimal('0.01'))
        
        result = {
            'userId': user_id,
            'subtotal': float(subtotal),
            'membershipRank': membership_rank,
            'discountPercentage': discount_percentage,
            'discountAmount': float(discount_amount),
            'loyaltyPoints': loyalty_points
        }
        
        print(f"Discount calculation result: {result}")
        return result
        
    except Exception as e:
        print(f"Error calculating discount: {str(e)}")
        # Return no discount on error
        return {
            'userId': event.get('userId'),
            'subtotal': event.get('subtotal', 0),
            'membershipRank': 'SILVER',
            'discountPercentage': 0,
            'discountAmount': 0.0,
            'loyaltyPoints': 0
        }
