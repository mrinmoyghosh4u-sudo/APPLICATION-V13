import re

with open(".github/workflows/android.yml", "r") as f:
    content = f.read()

# Replace the block that echoes secrets to .env
old_block = """          echo "ANGEL_ONE_API_KEY=$ANGEL_ONE_API_KEY" > .env
          echo "ANGEL_REDIRECT_URI=$ANGEL_REDIRECT_URI" >> .env
          echo "DHAN_CLIENT_ID=$DHAN_CLIENT_ID" >> .env
          echo "DHAN_API_KEY=$DHAN_API_KEY" >> .env
          echo "DHAN_CLIENT_SECRET=$DHAN_CLIENT_SECRET" >> .env
          echo "DHAN_REDIRECT_URI=$DHAN_REDIRECT_URI" >> .env"""

new_block = """          echo "ANGEL_ONE_API_KEY=$ANGEL_ONE_API_KEY" > .env
          echo "ANGEL_REDIRECT_URI=$ANGEL_REDIRECT_URI" >> .env
          echo "DHAN_CLIENT_ID=$DHAN_CLIENT_ID" >> .env
          echo "DHAN_API_KEY=$DHAN_API_KEY" >> .env
          echo "DHAN_CLIENT_SECRET=$DHAN_CLIENT_SECRET" >> .env
          echo "DHAN_REDIRECT_URI=$DHAN_REDIRECT_URI" >> .env
          echo "UPSTOX_API_KEY=${{ secrets.UPSTOX_API_KEY }}" >> .env
          echo "UPSTOX_API_SECRET=${{ secrets.UPSTOX_API_SECRET }}" >> .env
          echo "UPSTOX_REDIRECT_URI=${{ secrets.UPSTOX_REDIRECT_URI }}" >> .env
          echo "FYERS_APP_ID=${{ secrets.FYERS_APP_ID }}" >> .env
          echo "FYERS_SECRET_ID=${{ secrets.FYERS_SECRET_ID }}" >> .env
          echo "FYERS_REDIRECT_URI=${{ secrets.FYERS_REDIRECT_URI }}" >> .env"""

content = content.replace(old_block, new_block)

with open(".github/workflows/android.yml", "w") as f:
    f.write(content)
