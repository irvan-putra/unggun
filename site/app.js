"use strict";
const $ = id => document.getElementById(id), KEY = "unggun.web.v1";
let apps = JSON.parse(localStorage.getItem(KEY) || "[]");
const save = () => localStorage.setItem(KEY, JSON.stringify(apps));
function parse(v) {
  v = v.trim();
  try { if (v.includes("obtainium://app/")) { const j = JSON.parse(decodeURIComponent(v.split("obtainium://app/")[1])); v = j.url || ""; } } catch (e) { return null; }
  const m = v.match(/^(?:https?:\/\/)?(?:www\.)?(?:github\.com\/)?([A-Za-z0-9_.-]+)\/([A-Za-z0-9_.-]+?)(?:\.git)?(?:[\/?#].*)?$/);
  return m ? m[1] + "/" + m[2] : null;
}
function add(v) {
  const r = parse(v);
  if (!r) { $("msg").textContent = "Not a GitHub project: use owner/repo or a github.com link."; return; }
  if (apps.some(a => a.repo.toLowerCase() === r.toLowerCase())) { $("msg").textContent = r + " is already listed."; return; }
  apps.push({repo: r, installed: "", pre: false}); save(); $("msg").textContent = ""; $("u").value = ""; render(); check(apps[apps.length - 1]);
}
async function latest(a) {
  const res = await fetch(`https://api.github.com/repos/${a.repo}/releases?per_page=30`, {headers: {Accept: "application/vnd.github+json"}});
  $("rl").textContent = "Lookups left this hour: " + (res.headers.get("x-ratelimit-remaining") ?? "?");
  if (res.status === 403 || res.status === 429) throw new Error("GitHub's lookup limit was reached. Try again later.");
  if (res.status === 404) throw new Error("No such public project.");
  if (!res.ok) throw new Error("GitHub returned " + res.status);
  const rel = (await res.json()).filter(r => !r.draft && (a.pre || !r.prerelease));
  if (!rel.length) throw new Error(a.pre ? "No releases yet." : "No stable releases. If it only has prereleases, tick \"Include prereleases\".");
  // newest first; fall back to an older release when the newest has no APK (same idea as the app)
  for (const r of rel) { const apks = (r.assets || []).filter(x => /\.apk$/i.test(x.name)); if (apks.length) return {tag: r.tag_name || r.name, date: r.published_at, pre: r.prerelease, url: r.html_url, apks}; }
  throw new Error("Releases found, but none has an APK file.");
}
async function check(a) { a.err = null; a.busy = true; render(); try { a.res = await latest(a); a.res.at = Date.now(); } catch (e) { a.err = e.message; a.res = null; } a.busy = false; render(); }
function el(t, props, ...kids) { const e = document.createElement(t); Object.assign(e, props || {}); for (const k of kids) e.append(k); return e; }
function render() {
  $("list").replaceChildren(...apps.map((a, i) => {
    const c = el("div", {className: "card"});
    c.append(el("b", {}, a.repo), " ", el("a", {href: "https://github.com/" + a.repo, textContent: "open", target: "_blank", rel: "noopener"}));
    const row = el("div", {className: "row"});
    const inst = el("input", {type: "text", placeholder: "version you have (optional)", value: a.installed, style: "max-width:230px"}); inst.onchange = () => { a.installed = inst.value.trim(); save(); render(); };
    const pre = el("input", {type: "checkbox", checked: a.pre}); pre.onchange = () => { a.pre = pre.checked; save(); check(a); };
    const lab = el("label", {}, pre, " Include prereleases");
    row.append(inst, lab); c.append(row);
    if (a.busy) c.append(el("p", {className: "mut", textContent: "Checking..."}));
    if (a.err) c.append(el("p", {className: "up", textContent: a.err}));
    if (a.res) {
      const r = a.res, same = a.installed && a.installed.replace(/^v/i, "") === String(r.tag).replace(/^v/i, "");
      c.append(el("p", {}, "Latest: ", el("b", {textContent: r.tag}), r.pre ? " (prerelease)" : "", " · " + (r.date || "").slice(0, 10) + " · ", el("a", {href: r.url, textContent: "release page", target: "_blank", rel: "noopener"})));
      if (a.installed) c.append(el("p", {className: same ? "ok" : "up", textContent: same ? "You have the latest." : "Different from what you have: update available."}));
      for (const x of r.apks) c.append(el("p", {}, el("a", {href: x.browser_download_url, textContent: x.name}), ` (${(x.size / 1048576).toFixed(1)} MB)`, x.digest ? el("code", {textContent: " " + x.digest}) : ""));
    }
    const rm = el("button", {className: "alt", textContent: "Check"}); rm.onclick = () => check(a);
    const del = el("button", {className: "alt", textContent: "Remove"}); del.onclick = () => { apps.splice(i, 1); save(); render(); };
    c.append(rm, del); return c; }));
}
$("add").onclick = () => add($("u").value); $("u").onkeydown = e => { if (e.key === "Enter") add($("u").value); };
$("ex").onclick = () => { const x = "irvan-putra/unggun"; if (!apps.some(a => a.repo === x)) { apps.push({repo: x, installed: "", pre: true}); save(); } render(); check(apps.find(a => a.repo === x)); };
$("all").onclick = async () => { for (const a of apps) await check(a); };
render();
