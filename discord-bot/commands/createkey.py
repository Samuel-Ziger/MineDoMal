import discord
from discord import app_commands
from discord.ext import commands
import secrets
import string

def setup(bot, keys_collection, authorized_ids, *args):
    @bot.tree.command(name='createkey', description='Create a new access key (Authorized only)')
    async def createkey(interaction: discord.Interaction):
        if interaction.user.id not in authorized_ids:
            await interaction.response.send_message('You are not authorized to use this command.', ephemeral=True)
            return
        
        alphabet = string.ascii_letters + string.digits
        key = ''.join(secrets.choice(alphabet) for _ in range(20))
        
        keys_collection.insert_one({
            'key': key,
            'created_by': interaction.user.id,
            'created_at': interaction.created_at,
            'used': False,
            'used_by': None,
            'used_at': None
        })
        
        await interaction.response.send_message(
            f'Key created successfully!\n`{key}`',
            ephemeral=True
        )

