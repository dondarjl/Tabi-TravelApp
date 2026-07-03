from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from routers import trips, users, follows, interactions, maps

load_dotenv()

app = FastAPI(title="Tabi API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(trips.router)
app.include_router(users.router)
app.include_router(follows.router)
app.include_router(interactions.router)
app.include_router(maps.router)

@app.get("/")
async def root():
    return {"message": "Tabi API funcionando"}

@app.get("/health")
async def health():
    return {"status": "ok"}