#!/bin/bash

# Crea la struttura della cartella
mkdir -p sensors_manager/{backend,frontend/templates,frontend/static}

# File di configurazione base
cat > sensors_manager/config.json <<EOF
{
  "mqtt_broker": "mosquitto",
  "mqtt_port": 1883,
  "influxdb_url": "http://influxdb:8086",
  "database": "sensors_db"
}
EOF

# Backend Python (FastAPI)
cat > sensors_manager/backend/main.py <<'EOF'
from fastapi import FastAPI, Request, Form
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
import json
from pathlib import Path

app = FastAPI()

# Configurazione percorsi
BASE_DIR = Path(__file__).parent
app.mount("/static", StaticFiles(directory=BASE_DIR / "../frontend/static"), name="static")
templates = Jinja2Templates(directory=BASE_DIR / "../frontend/templates")

# File di configurazione
CONFIG_FILE = BASE_DIR / "../config.json"

# Carica i sensori esistenti
def load_sensors():
    try:
        with open(BASE_DIR / "sensors.json", "r") as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return []

# Salva i sensori
def save_sensors(sensors):
    with open(BASE_DIR / "sensors.json", "w") as f:
        json.dump(sensors, f, indent=2)

@app.get("/", response_class=HTMLResponse)
async def read_root(request: Request):
    sensors = load_sensors()
    return templates.TemplateResponse("index.html", {"request": request, "sensors": sensors})

@app.post("/add_sensor")
async def add_sensor(
    name: str = Form(...),
    min_threshold: float = Form(...),
    max_threshold: float = Form(...)
):
    sensors = load_sensors()
    
    new_sensor = {
        "name": name,
        "min_threshold": min_threshold,
        "max_threshold": max_threshold,
        "topic": f"sensors/{name.lower().replace(' ', '_')}"
    }
    
    sensors.append(new_sensor)
    save_sensors(sensors)
    
    return {"status": "success", "sensor": new_sensor}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
EOF

# Pagina HTML frontend
cat > sensors_manager/frontend/templates/index.html <<'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>Gestione Sensori</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container mt-5">
        <h1 class="mb-4">Gestione Sensori</h1>
        
        <!-- Form aggiunta sensore -->
        <div class="card mb-4">
            <div class="card-header">
                <h5>Aggiungi Nuovo Sensore</h5>
            </div>
            <div class="card-body">
                <form action="/add_sensor" method="post">
                    <div class="mb-3">
                        <label for="name" class="form-label">Nome Sensore</label>
                        <input type="text" class="form-control" id="name" name="name" required>
                    </div>
                    <div class="mb-3">
                        <label for="min_threshold" class="form-label">Soglia Minima</label>
                        <input type="number" step="0.1" class="form-control" id="min_threshold" name="min_threshold" required>
                    </div>
                    <div class="mb-3">
                        <label for="max_threshold" class="form-label">Soglia Massima</label>
                        <input type="number" step="0.1" class="form-control" id="max_threshold" name="max_threshold" required>
                    </div>
                    <button type="submit" class="btn btn-primary">Aggiungi Sensore</button>
                </form>
            </div>
        </div>

        <!-- Lista sensori esistenti -->
        <div class="card">
            <div class="card-header">
                <h5>Sensori Configurati</h5>
            </div>
            <div class="card-body">
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <th>Nome</th>
                            <th>Soglia Min</th>
                            <th>Soglia Max</th>
                            <th>Topic MQTT</th>
                        </tr>
                    </thead>
                    <tbody>
                        {% for sensor in sensors %}
                        <tr>
                            <td>{{ sensor.name }}</td>
                            <td>{{ sensor.min_threshold }}</td>
                            <td>{{ sensor.max_threshold }}</td>
                            <td>{{ sensor.topic }}</td>
                        </tr>
                        {% endfor %}
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
EOF

# Creazione Dockerfile per il backend
cat > sensors_manager/backend/Dockerfile <<'EOF'
FROM python:3.9-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

CMD ["python", "main.py"]
EOF

# File requirements per Python
cat > sensors_manager/backend/requirements.txt <<EOF
fastapi>=0.95.0
uvicorn>=0.21.1
python-multipart>=0.0.6
jinja2>=3.1.2
EOF

# File README.md
cat > sensors_manager/README.md <<'EOF'
# Gestore Sensori Autonomo

## Struttura del progetto
- `backend/`: Servizio FastAPI per la gestione dei sensori
- `frontend/`: Interfaccia web minimale
- `config.json`: Configurazione connessioni

## Avvio
```bash
docker compose up -d