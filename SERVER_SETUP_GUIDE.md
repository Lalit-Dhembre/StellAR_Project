# StellAR Server Setup Guide 🚀

This guide covers everything you need to set up, run, and expose the StellAR backend server.

---

## 1. Prerequisites
Before starting, ensure you have the following installed on your Windows machine:
- **Python 3.10+**
- **Docker Desktop** (Required for Redis / Celery background tasks)
- **Git**

## 2. Docker Desktop & Redis Setup
Redis is required for managing asynchronous background tasks (like the 3D model generation pipeline via Celery).

1. Open **Docker Desktop** and wait for the engine to start.
2. Open a new Command Prompt or PowerShell window and run:
   ```bash
   docker run -d -p 6379:6379 --name stellar-redis redis
   ```
   *(If you've already created the container previously, you can simply start it from the Docker Desktop UI).*

## 3. Environment & Database Configuration
1. In the `StellAR-Server` directory, duplicate `.env.example` and rename it to `.env`.
2. Fill in the required keys:
   ```ini
   SUPABASE_URL=your_supabase_url
   SUPABASE_KEY=your_supabase_anon_key
   JWT_SECRET_KEY=your_secret_key
   
   # External APIs
   ELEVENLABS_API_KEY=your_elevenlabs_key
   GOOGLE_CUSTOM_SEARCH_API_KEY=your_google_search_key
   ```
   *(Make sure to replace these with your actual keys from the Supabase dashboard and relevant API providers).*

## 4. Python Environment setup
1. Open a terminal in the `StellAR-Server` directory.
2. Create and activate a virtual environment:
   ```bash
   python -m venv venv
   .\venv\Scripts\activate
   ```
3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## 5. Launching ComfyUI (3D Generation Engine)
ComfyUI powers the image-to-3D pipeline and needs to be running in the background.

1. Navigate to your ComfyUI portable installation folder.
2. Double-click **`run_nvidia_gpu.bat`** (or `run_nvidia_gpu_fast_fp16_accumulation.bat` if preferred).
3. Wait for it to load completely. Ensure it is accessible at `http://127.0.0.1:8188`.
   *(Note: The server is hardcoded to look for ComfyUI's output in the portable folder's output directory).*

## 6. Starting Celery Worker (Optional but recommended)
If you are actively using the task queue defined in `modules/tasks.py`:
1. Open a **new** terminal in `StellAR-Server` and activate the virtual environment (`.\venv\Scripts\activate`).
2. Run the Celery worker (on Windows, `-P solo` is typically required):
   ```bash
   celery -A modules.tasks.celery_app worker --loglevel=info -P solo
   ```

## 7. Starting the Server & Exposing it with Ngrok
The mobile app and Unity client need a public HTTPS endpoint to connect to your local server.

1. Navigate to your `StellAR-Server` directory.
2. Double-click the **`expose_server.bat`** script.
   * If this is your first time, press `1` to configure your Ngrok Auth Token (you can paste your token when prompted).
   * Otherwise, press `2` to start the server.
3. **What this script does:**
   - Automatically activates your `venv`.
   - Starts the Flask server (`app.py`) on port 5000 in a new window.
   - Starts the Ngrok tunnel with your fixed domain.

> [!IMPORTANT]
> The server will be exposed at your static Ngrok domain (e.g., `https://chun-nonimpulsive-nondeficiently.ngrok-free.dev`). Ensure your mobile app/client `.env` files are updated to point to this exact URL!

## Setup Summary Checklist ✅
- [ ] Docker Desktop running with Redis (`:6379`).
- [ ] ComfyUI running via `run_nvidia_gpu.bat` (`:8188`).
- [ ] Celery worker running (if testing async tasks).
- [ ] `expose_server.bat` running (Hosts Flask + Ngrok).
- [ ] Clients configured with the Ngrok URL.
