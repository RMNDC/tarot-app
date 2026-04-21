import os
import random

import requests
from flask import Flask, jsonify, send_file

app = Flask(__name__)

TAROT_API_URL = "https://tarotapi.dev/api/v1/cards"
POSITIONS = ["Past", "Present", "Future"]
PORT = int(os.environ.get("PORT", os.environ.get("TAROT_FLASK_PORT", "5051")))
HOST = "0.0.0.0" if os.environ.get("REPL_ID") else "127.0.0.1"


def fetch_cards():
    try:
        response = requests.get(TAROT_API_URL, timeout=10)
        response.raise_for_status()
        return response.json().get("cards", [])
    except requests.RequestException:
        return None


def build_spread(cards):
    spread = []

    for position, card in zip(POSITIONS, random.sample(cards, 3)):
        reversed_card = random.choice([True, False])
        spread.append(
            {
                "position": position,
                "name": card.get("name", "Unknown"),
                "orientation": "Reversed" if reversed_card else "Upright",
                "meaning": card.get("meaning_rev" if reversed_card else "meaning_up")
                or "No meaning found.",
            }
        )

    return spread


@app.route("/")
def home():
    return send_file("index.html")


@app.route("/draw")
def draw():
    cards = fetch_cards()

    if not cards:
        return jsonify({"error": "Failed to fetch cards from the Tarot API."}), 500

    return jsonify({"spread": build_spread(cards)})


if __name__ == "__main__":
    print(f"Tarot demo running on http://{HOST}:{PORT}")
    app.run(debug=True, host=HOST, port=PORT)
