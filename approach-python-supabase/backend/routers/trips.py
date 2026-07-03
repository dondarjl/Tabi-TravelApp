from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional, List
from datetime import date
from database import get_client

router = APIRouter(prefix="/trips", tags=["trips"])

class TripCreate(BaseModel):
    title: str
    destination: str
    start_date: date
    end_date: date
    total_cost: int
    currency: str = "EUR"
    cover_photo_url: Optional[str] = None
    trip_type: Optional[List[str]] = []

@router.get("/")
async def get_trips():
    async with get_client() as client:
        response = await client.get(
            "/rest/v1/trips",
            params={
                "select": "*",
                "is_published": "eq.true",
                "order": "created_at.desc"
            }
        )
        if response.status_code != 200:
            raise HTTPException(status_code=response.status_code, detail=response.text)
        return response.json()

@router.get("/{trip_id}")
async def get_trip(trip_id: str):
    async with get_client() as client:
        response = await client.get(
            "/rest/v1/trips",
            params={
                "select": "*",
                "id": f"eq.{trip_id}"
            }
        )
        if response.status_code != 200:
            raise HTTPException(status_code=response.status_code, detail=response.text)
        data = response.json()
        if not data:
            raise HTTPException(status_code=404, detail="Viaje no encontrado")
        return data[0]

@router.post("/")
async def create_trip(trip: TripCreate, user_id: str):
    async with get_client() as client:
        response = await client.post(
            "/rest/v1/trips",
            json={
                "user_id": user_id,
                "title": trip.title,
                "destination": trip.destination,
                "start_date": str(trip.start_date),
                "end_date": str(trip.end_date),
                "total_cost": trip.total_cost,
                "currency": trip.currency,
                "cover_photo_url": trip.cover_photo_url,
                "trip_type": trip.trip_type,
                "is_published": False
            },
            headers={"Prefer": "return=representation"}
        )
        if response.status_code not in (200, 201):
            raise HTTPException(status_code=response.status_code, detail=response.text)
        return response.json()[0]