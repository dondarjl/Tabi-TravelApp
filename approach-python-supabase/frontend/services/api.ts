const API_URL = 'https://web-production-cd7a6.up.railway.app';

export async function getTrips() {
  const res = await fetch(`${API_URL}/trips/`);
  return res.json();
}

export async function getTrip(id: string) {
  const res = await fetch(`${API_URL}/trips/${id}`);
  return res.json();
}

export async function createTrip(data: any) {
  const res = await fetch(`${API_URL}/trips/`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return res.json();
}