#!/usr/bin/env python3
import sys
import os
import base64
import subprocess
from datetime import datetime

ENV_FILE_PATH = "/storage/emulated/0/anchorage.env"
LOG_FILE_PATH = "/storage/emulated/0/.anchorage/logs/cli.log"
CRYPTO_KEY = b"AnchorageShellEcosystemKeyShared"

# Truncated to exactly 16 elements to match the Kotlin Android implementation 1:1
S_BOX = [0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76]

def simple_block_crypt(data, key):
    out = bytearray(data)
    for i in range(len(out)):
        out[i] ^= key[i % len(key)] ^ S_BOX[i % 16]
    return bytes(out)

def encrypt_env(plain_text):
    raw_bytes = plain_text.encode('utf-8')
    crypt_bytes = simple_block_crypt(raw_bytes, CRYPTO_KEY)
    return base64.b64encode(crypt_bytes).decode('utf-8').replace('\n', '')

def decrypt_env(cipher_text):
    crypt_bytes = base64.b64decode(cipher_text.encode('utf-8'))
    raw_bytes = simple_block_crypt(crypt_bytes, CRYPTO_KEY)
    return raw_bytes.decode('utf-8')

def write_log(action):
    try:
        os.makedirs(os.path.dirname(LOG_FILE_PATH), exist_ok=True)
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        with open(LOG_FILE_PATH, "a") as log_file:
            log_file.write(f"[{timestamp}] {action}\n")
    except: pass

def load_registry():
    if not os.path.exists(ENV_FILE_PATH):
        print("Error: anchorage.env not initialized. Please run the Setup App first.")
        sys.exit(1)
    try:
        with open(ENV_FILE_PATH, "r") as f:
            encrypted_data = f.read().strip()
        decrypted_text = decrypt_env(encrypted_data)
        registry = {}
        for line in decrypted_text.splitlines():
            if "=" in line:
                k, v = line.split("=", 1)
                registry[k.strip()] = v.strip()
        
        if "DEFAULT_PACK" not in registry:
            registry["DEFAULT_PACK"] = "/storage/emulated/0/.defaultPack"
        return registry
    except Exception as e:
        print(f"Error: Failed to safely parse encrypted env registry mapping. ({e})")
        sys.exit(1)

def save_registry(registry):
    try:
        lines = [f"{k}={v}" for k, v in registry.items()]
        plain_text = "\n".join(lines)
        encrypted_data = encrypt_env(plain_text)
        with open(ENV_FILE_PATH, "w") as f:
            f.write(encrypted_data)
    except Exception as e:
        print(f"Error: Failed to write configuration safely. ({e})")
        sys.exit(1)

def main():
    if len(sys.argv) < 2 or sys.argv[1] == "--help":
        print("Anchorage Universal CLI - Operational")
        print("Commands:")
        print('  addEnv "NAME" "VALUE"       - Add a new variable')
        print('  upEnv "NAME" "VALUE"        - Update an existing variable')
        print('  deleteEnv "NAME"            - Delete an environmental variable')
        print('  viewEnv "NAME"              - View specific variable')
        print('  showEnv                     - Print all decoded variables')
        print("\nProxy Execution:")
        print("  <command> [args]            - Execute tools via the DEFAULT_PACK pipeline")
        sys.exit(0)

    cmd = sys.argv[1]
    write_log(f"Command Invocated: {' '.join(sys.argv[1:])}")

    if cmd == "showEnv":
        reg = load_registry()
        print("\n=== Decrypted Environment Registry ===")
        for k, v in reg.items():
            print(f"{k} -> {v}")
        print("======================================")
        sys.exit(0)

    elif cmd == "viewEnv":
        if len(sys.argv) < 3:
            print('Error: Missing parameters. Usage: anchorage viewEnv "VARIABLE_NAME"')
            sys.exit(1)
        reg = load_registry()
        if sys.argv[2] in reg:
            print(reg[sys.argv[2]])
        else:
            print(f"Error: Target '{sys.argv[2]}' not allocated.")
            sys.exit(1)

    elif cmd == "addEnv":
        if len(sys.argv) < 4:
            print('Error: Syntax invalid. Usage: anchorage addEnv "NAME" "VALUE"')
            sys.exit(1)
        reg = load_registry()
        k, v = sys.argv[2], sys.argv[3]
        if k in reg:
            print(f"Error: Key '{k}' already exists. Use 'upEnv' to update its value.")
            sys.exit(1)
        reg[k] = v
        save_registry(reg)
        print(f"Success: Registered variable '{k}'.")

    elif cmd == "upEnv":
        if len(sys.argv) < 4:
            print('Error: Syntax invalid. Usage: anchorage upEnv "NAME" "VALUE"')
            sys.exit(1)
        reg = load_registry()
        k, v = sys.argv[2], sys.argv[3]
        if k == "DEFAULT_PACK":
            print("Error: DEFAULT_PACK is a core ecosystem constant and cannot be modified.")
            sys.exit(1)
        if k not in reg:
            print(f"Error: Key '{k}' not found. Use 'addEnv' to create it.")
            sys.exit(1)
        reg[k] = v
        save_registry(reg)
        print(f"Success: Updated mapped value for '{k}'.")

    elif cmd == "deleteEnv":
        if len(sys.argv) < 3:
            print('Error: Syntax invalid. Usage: anchorage deleteEnv "NAME"')
            sys.exit(1)
        reg = load_registry()
        k = sys.argv[2]
        if k == "DEFAULT_PACK":
            print("Error: Operation aborted. Cannot delete core system anchor DEFAULT_PACK.")
            sys.exit(1)
        if k not in reg:
            print(f"Error: Key '{k}' does not exist.")
            sys.exit(1)
        del reg[k]
        save_registry(reg)
        print(f"Success: Deleted variable '{k}'.")

    else:
        # UNIVERSAL PROXY LAYER (Flawless NoExec Bypass)
        reg = load_registry()
        default_pack = reg.get("DEFAULT_PACK", "/storage/emulated/0/.defaultPack")
        
        custom_env = os.environ.copy()
        custom_env["PATH"] = f"{default_pack}/bin:{default_pack}/usr/bin:{custom_env.get('PATH', '')}"
        for k, v in reg.items():
            custom_env[k] = v
            
        proxy_args = sys.argv[1:]
        target_cmd = proxy_args[0]
        script_path = os.path.join(default_pack, "bin", target_cmd)
        
        # If it exists in our Anchorage path, bypass Android's block
        if os.path.exists(script_path):
            try:
                with open(script_path, 'r') as f:
                    first_line = f.readline().strip()
                    if first_line.startswith("#!"):
                        interpreter = first_line[2:].split()[0].split('/')[-1]
                        if interpreter == "env":
                            interpreter = first_line[2:].split()[1]
                        # Rewrite args to feed the script into the interpreter
                        proxy_args = [interpreter, script_path] + proxy_args[1:]
                    else:
                        proxy_args = ["sh", script_path] + proxy_args[1:]
            except Exception:
                proxy_args = ["sh", script_path] + proxy_args[1:]
                
        try:
            subprocess.run(proxy_args, env=custom_env)
        except PermissionError:
            print(f"Error: Android blocked execution of compiled binary '{proxy_args[0]}'. Compiled binaries must run from internal app data, or use interpreter scripts in shared storage.")
            sys.exit(1)
        except FileNotFoundError:
            print(f"Error: Tool '{proxy_args[0]}' could not be located in standard paths.")
            sys.exit(1)

if __name__ == "__main__":
    main()
