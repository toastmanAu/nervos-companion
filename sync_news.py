import json
import os
import subprocess
import time
from datetime import datetime

def parse_iso_date(date_str):
    # Parse ISO dates from Github API like: "2026-03-09T08:58:29Z"
    try:
        dt = datetime.strptime(date_str.strip(), "%Y-%m-%dT%H:%M:%SZ")
        return int(time.mktime(dt.timetuple())) * 1000
    except Exception:
        return int(time.time() * 1000)

def fetch_discussions_via_gh(owner, name):
    query = """
    query($owner: String!, $name: String!) {
      repository(owner: $owner, name: $name) {
        discussions(first: 20, orderBy: {field: CREATED_AT, direction: DESC}) {
          nodes {
            title
            url
            createdAt
            bodyText
          }
        }
      }
    }
    """
    cmd = [
        "gh", "api", "graphql",
        "-F", f"owner={owner}",
        "-F", f"name={name}",
        "-f", f"query={query}"
    ]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        data = json.loads(result.stdout)
        nodes = data.get("data", {}).get("repository", {}).get("discussions", {}).get("nodes", [])
        return nodes
    except Exception as e:
        print(f"Error calling GitHub API for {owner}/{name}: {e}")
        # Print stderr if available
        if 'result' in locals() and result.stderr:
            print(f"Subprocess stderr: {result.stderr}")
        return []

def main():
    all_items = []

    # 1. Fetch CKB Discussions
    print("Fetching CKB Discussions via GitHub CLI...")
    ckb_nodes = fetch_discussions_via_gh("nervosnetwork", "ckb")
    print(f"Fetched {len(ckb_nodes)} discussions for ckb.")
    for node in ckb_nodes:
        title = node.get("title", "")
        summary = node.get("bodyText", "")
        if len(summary) > 200:
            summary = summary[:200] + "..."
            
        tags = ["discussion", "core"]
        if "devlog" in title.lower():
            tags.append("devlog")
        if "release" in title.lower() or "rc" in title.lower():
            tags.append("release")
            
        all_items.append({
            "title": title,
            "url": node.get("url", ""),
            "source": "CKB Discussions",
            "summary": summary,
            "publishedAt": parse_iso_date(node.get("createdAt", "")),
            "tags": tags
        })

    # 2. Fetch Fiber Discussions
    print("Fetching Fiber Discussions via GitHub CLI...")
    fiber_nodes = fetch_discussions_via_gh("nervosnetwork", "fiber")
    print(f"Fetched {len(fiber_nodes)} discussions for fiber.")
    for node in fiber_nodes:
        title = node.get("title", "")
        summary = node.get("bodyText", "")
        if len(summary) > 200:
            summary = summary[:200] + "..."
            
        tags = ["discussion", "fiber", "layer2"]
        if "devlog" in title.lower():
            tags.append("devlog")
        if "release" in title.lower() or "rc" in title.lower():
            tags.append("release")
            
        all_items.append({
            "title": title,
            "url": node.get("url", ""),
            "source": "Fiber Discussions",
            "summary": summary,
            "publishedAt": parse_iso_date(node.get("createdAt", "")),
            "tags": tags
        })

    # Sort descending by date
    all_items.sort(key=lambda x: x["publishedAt"], reverse=True)
    
    # Save directory
    os.makedirs("config", exist_ok=True)
    output_file = "config/featured_links.json"

    # Write JSON output
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(all_items, f, indent=2, ensure_ascii=False)
        
    print(f"Successfully compiled {len(all_items)} total discussion items into {output_file}")

if __name__ == "__main__":
    main()
