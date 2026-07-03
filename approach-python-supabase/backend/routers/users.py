from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from database import get_client

router = APIRouter(prefix="/users", tags=["users"])

class UserCreate(BaseModel):
    id: str
    username: str
    bio: Optional[str] = None
    avatar_url: Optional[str] = None
    is_private: bool = False

@router.post("/")
async def create_user(user: UserCreate):
    async with get_client() as client:
        response = await client.post(
            "/rest/v1/users",
            json=user.model_dump(),
            headers={"Prefer": "return=representation"}
        )
        if response.status_code not in (200, 201):
            raise HTTPException(status_code=response.status_code, detail=response.text)
        return response.json()[0]

@router.get("/{user_id}")
async def get_user(user_id: str):
    async with get_client() as client:
        response = await client.get(
            "/rest/v1/users",
            params={"select": "*", "id": f"eq.{user_id}"}
        )
        if response.status_code != 200:
            raise HTTPException(status_code=response.status_code, detail=response.text)
        data = response.json()
        if not data:
            raise HTTPException(status_code=404, detail="Usuario no encontrado")
        return data[0]

@router.get("/{user_id}/trips")
async def get_user_trips(user_id: str):
    async with get_client() as client:
        response = await client.get(
            "/rest/v1/trips",
            params={
                "select": "*",
                "user_id": f"eq.{user_id}",
                "is_published": "eq.true",
                "order": "created_at.desc"
            }
        )
        if response.status_code != 200:
            raise HTTPException(status_code=response.status_code, detail=response.text)
        return response.json()