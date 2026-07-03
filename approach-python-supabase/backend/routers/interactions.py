from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from database import get_client

router = APIRouter(tags=["interactions"])

@router.post("/trips/{trip_id}/like")
async def like_trip(trip_id: str, user_id: str):
    async with get_client() as client:
        response = await client.post(
            "/rest/v1/trip_likes",
            json={"trip_id": trip_id, "user_id": user_id},
            headers={"Prefer": "return=representation"}
        )
        if response.status_code not in (200, 201):
            raise HTTPException(status_code=response.status_code, detail=response.text)
        return {"message": "Like añadido"}

@router.delete("/trips/{trip_id}/like")
async def unlike_trip(trip_id: str, user_id: str):
    async with get_client() as client:
        response = await client.delete(
            "/rest/v1/trip_likes",
            params={"trip_id": f"eq.{trip_id}", "user_id": f"eq.{user_id}"}
        )
        if response.status_code not in (200, 204):
            raise HTTPException(status_code=response.status_code, detail=response.text)
        return {"message": "Like eliminado"}

class CommentCreate(BaseModel):
    content: str

@router.post("/trips/{trip_id}/comments")
async def add_comment(trip_id: str, user_id: str, comment: CommentCreate):
    async with get_client() as client:
        response = await client.post(
            "/rest/v1/trip_comments",
            json={"trip_id": trip_id, "user_id": user_id, "content": comment.content},
            headers={"Prefer": "return=representation"}
        )
        if response.status_code not in (200, 201):
            raise HTTPException(status_code=response.status_code, detail=response.text)
        return response.json()[0]

@router.get("/trips/{trip_id}/comments")
async def get_comments(trip_id: str):
    async with get_client() as client:
        response = await client.get(
            "/rest/v1/trip_comments",
            params={
                "select": "*",
                "trip_id": f"eq.{trip_id}",
                "order": "created_at.asc"
            }
        )
        if response.status_code != 200:
            raise HTTPException(status_code=response.status_code, detail=response.text)
        return response.json()