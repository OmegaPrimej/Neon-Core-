import os
import json
import time
import threading
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import Request, urlopen

LWA_TOKEN_URL = "https://api.amazon.com/auth/o2/token"
TOKEN_FILE = Path("avs_tokens.json")
CLIENT_ID = os.environ.get("AVS_CLIENT_ID", "").strip()
CLIENT_SECRET = os.environ.get("AVS_CLIENT_SECRET", "").strip()
REDIRECT_URI = os.environ.get("AVS_REDIRECT_URI", "").strip()

_lock = threading.Lock()

def _post_token(data: dict) -> dict:
    req = Request(
        LWA_TOKEN_URL,
        data=urlencode(data).encode(),
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    with urlopen(req, timeout=20) as res:
        body = json.loads(res.read().decode())
        if res.status!= 200:
            raise RuntimeError(f"LWA token error: {body}")
        return body

def exchange_authorization_code(code: str) -> dict:
    """ Run ONCE after the user completes Amazon login. Do not reuse `code`; authorization codes are single-use. """
    if not CLIENT_ID:
        raise RuntimeError("Set AVS_CLIENT_ID in env")
    response = _post_token({
        "grant_type": "authorization_code",
        "code": code,
        "client_id": CLIENT_ID,
        "client_secret": CLIENT_SECRET,
        "redirect_uri": REDIRECT_URI,
    })
    save_tokens(response)
    return response

def load_tokens() -> dict:
    if not TOKEN_FILE.exists():
        raise RuntimeError("No saved tokens. Run exchange_authorization_code() once first.")
    return json.loads(TOKEN_FILE.read_text())

def save_tokens(response: dict) -> None:
    expires_at = time.time() + int(response.get("expires_in", 3600))
    TOKEN_FILE.write_text(json.dumps({
        "access_token": response["access_token"],
        "refresh_token": response["refresh_token"],
        "expires_at": expires_at,
    }, indent=2))

def get_valid_access_token() -> str:
    """ Thread-safe access-token getter for your AVS voice runtime. Refreshes about 5 minutes before expiration. """
    with _lock:
        tokens = load_tokens()
        if time.time() < tokens["expires_at"] - 300:
            return tokens["access_token"]
        response = _post_token({
            "grant_type": "refresh_token",
            "refresh_token": tokens["refresh_token"],
            "client_id": CLIENT_ID,
            "client_secret": CLIENT_SECRET,
            "redirect_uri": REDIRECT_URI,
        })
        save_tokens(response)
        return response["access_token"]

if __name__ == "__main__":
    import sys
    if len(sys.argv) == 2:
        result = exchange_authorization_code(sys.argv[1])
        print("Saved refresh token. Access token expires in:", result["expires_in"], "seconds")
        print(f"Saved to {TOKEN_FILE}")
    else:
        print(get_valid_access_token())
