import os
import asyncio
import discord
from discord import app_commands
from discord.ext import commands
from pymongo import MongoClient
from dotenv import load_dotenv
import importlib.util
import sys
from pathlib import Path

load_dotenv()

# Configuration
TOKEN = os.getenv('DISCORD_TOKEN')
MONGODB_URI = os.getenv('MONGODB_URI', 'mongodb://localhost:27017/')
AUTHORIZED_IDS = [int(id.strip()) for id in os.getenv('AUTHORIZED_IDS', '').split(',') if id.strip()]

# MongoDB setup
mongo_client = MongoClient(MONGODB_URI)
db = mongo_client['abysrat_bot']
keys_collection = db['keys']

# Bot setup
intents = discord.Intents.default()
intents.message_content = True
bot = commands.Bot(command_prefix='!', intents=intents)

# Project paths
PROJECT_DIR = Path(__file__).parent.parent
BUILD_DIR = PROJECT_DIR / 'build' / 'libs'
MAINMOD_PATH = PROJECT_DIR / 'src' / 'main' / 'java' / 'dev' / 'nfo' / 'MainMod.java'
MCMODINFO_PATH = PROJECT_DIR / 'src' / 'main' / 'resources' / 'mcmod.info'

@bot.event
async def on_ready():
    print(f'Bot connected as {bot.user} (ID: {bot.user.id})')
    print('Syncing commands...')
    try:
        synced = await bot.tree.sync()
        print(f'Synced {len(synced)} command(s)')
    except Exception as e:
        print(f'Error syncing commands: {e}')

# Load commands from ./commands directory
async def load_commands():
    commands_dir = Path(__file__).parent / 'commands'
    if commands_dir.exists():
        for file in commands_dir.glob('*.py'):
            if file.name == '__init__.py':
                continue
            try:
                module_name = f'commands.{file.stem}'
                spec = importlib.util.spec_from_file_location(module_name, file)
                module = importlib.util.module_from_spec(spec)
                sys.modules[module_name] = module
                spec.loader.exec_module(module)
                if hasattr(module, 'setup'):
                    module.setup(bot, keys_collection, AUTHORIZED_IDS, PROJECT_DIR, BUILD_DIR, MAINMOD_PATH, MCMODINFO_PATH)
                print(f'Loaded command: {file.stem}')
            except Exception as e:
                print(f'Error loading command {file.stem}: {e}')

@bot.event
async def setup_hook():
    await load_commands()

if __name__ == '__main__':
    bot.run(TOKEN)

