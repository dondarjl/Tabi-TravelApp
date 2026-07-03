from fastapi import APIRouter, HTTPException
from database import get_client

router = APIRouter(prefix="/follows", tags=["follows"])

@router.post("/")
async def follow_user(follower_id: str, following_id: str):
    async with get_client() as client:
        response = await client.post(
            "/rest/v1/follows",
            json={
                "follower_id": follower_id,
                "following_id": following_id,
                "status": "accepted"
            },
            headers={"Prefer": "return=representation"}
        )
        if response.status_code not in (200, 201):
            raise HTTPException(status_code=response.status_code, detail=response.text)
        return response.json()[0]

@router.delete("/")
async def unfollow_user(follower_id: str, following_id: str):
    async with get_client() as client:
        response = await client.delete(
            "/rest/v1/follows",
            params={
                "follower_id": f"eq.{follower_id}",
                "following_id": f"eq.{following_id}"
            }
        )
        if response.status_code not in (200, 204):
            raise HTTPException(status_code=response.status_code, detail=response.text)
        return {"message": "Dejaste de seguir al usuario"}

@router.get("/{user_id}/feed")
async def get_followed_feed(user_id: str):
    async with get_client() as client:
        follows_response = await client.get(
            "/rest/v1/follows",
            params={
                "select": "following_id",
                "follower_id": f"eq.{user_id}",
                "status": "eq.accepted"
            }
        )
        if follows_response.status_code != 200:
            raise HTTPException(status_code=follows_response.status_code, detail=follows_response.text)
        
        following_ids = [f["following_id"] for f in follows_response.json()]
        
        if not following_ids:
            return []
        
        ids_filter = "(" + ",".join(following_ids) + ")"
        trips_response = await client.get(
            "/rest/v1/trips",
            params={
                "select": "*",
                "user_id": f"in.{ids_filter}",
                "is_published": "eq.true",
                "order": "created_at.desc"
            }
        )
        if trips_response.status_code != 200:
            raise HTTPException(status_code=trips_response.status_code, detail=trips_response.text)
        return trips_response.json()