from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from database import get_client
from dotenv import load_dotenv
import os
import json
import httpx
from groq import Groq

load_dotenv()

groq_client = Groq(api_key=os.getenv("GROQ_API_KEY"))

router = APIRouter(prefix="/maps", tags=["maps"])

class MapInput(BaseModel):
    trip_id: str
    trip_day_id: str
    text: str

async def extract_places(text: str) -> list[str]:
    response = groq_client.chat.completions.create(
        model="llama-3.1-8b-instant",
        messages=[
            {
                "role": "system",
                "content": "Extrae los nombres de lugares visitados del texto. Devuelve SOLO un array JSON de strings con los nombres, sin texto adicional ni backticks. Ejemplo: [\"Parque del Retiro\", \"Museo del Prado\"]"
            },
            {"role": "user", "content": text}
        ]
    )
    raw = response.choices[0].message.content.strip()
    return json.loads(raw)

async def geocode_place(place_name: str) -> dict:
    async with httpx.AsyncClient() as client:
        response = await client.get(
            "https://nominatim.openstreetmap.org/search",
            params={
                "q": place_name,
                "format": "json",
                "limit": 1
            },
            headers={"User-Agent": "tabi-app/1.0"}
        )
        data = response.json()
        if data:
            return {"lat": float(data[0]["lat"]), "lng": float(data[0]["lon"])}
    return None

@router.post("/geocode")
async def geocode_day(input: MapInput):
    places = await extract_places(input.text)
    
    results = []
    async with get_client() as client:
        for i, place_name in enumerate(places):
            coords = await geocode_place(place_name)
            if coords:
                response = await client.post(
                    "/rest/v1/map_places",
                    json={
                        "trip_id": input.trip_id,
                        "trip_day_id": input.trip_day_id,
                        "place_name": place_name,
                        "latitude": coords["lat"],
                        "longitude": coords["lng"],
                        "place_type": "stop",
                        "sort_order": i
                    },
                    headers={"Prefer": "return=representation"}
                )
                if response.status_code in (200, 201):
                    results.append(response.json()[0])
    
    return {"places_found": len(places), "places_geocoded": len(results), "places": results}