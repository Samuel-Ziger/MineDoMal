import discord
from discord import app_commands
from discord.ext import commands
from datetime import datetime

def setup(bot, keys_collection, authorized_ids, *args):
    @bot.tree.command(name='redeem', description='Redeem an access key')
    @app_commands.describe(key='Your access key to redeem')
    async def redeem(interaction: discord.Interaction, key: str):
        key_doc = keys_collection.find_one({'key': key})
        
        if not key_doc:
            await interaction.response.send_message('Invalid key.', ephemeral=True)
            return
        
        # Check if user already has a redeemed key
        user_has_key = keys_collection.find_one({
            'redeemed_by': interaction.user.id
        })
        
        if user_has_key:
            await interaction.response.send_message('You can only redeem one key at a time. Use your current key with /build command.', ephemeral=True)
            return
        
        # Check if key was already redeemed by someone else
        if key_doc.get('redeemed_by') and key_doc.get('redeemed_by') != interaction.user.id:
            await interaction.response.send_message('This key was already redeemed by another user.', ephemeral=True)
            return
        
        # Mark key as redeemed (but not used yet)
        keys_collection.update_one(
            {'key': key},
            {'$set': {
                'redeemed_by': interaction.user.id,
                'redeemed_at': datetime.utcnow()
            }}
        )
        
        await interaction.response.send_message(
            f'Key redeemed successfully!',
            ephemeral=True
        )
