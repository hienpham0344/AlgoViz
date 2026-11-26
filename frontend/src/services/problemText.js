// Imported problems may contain HTML. Keep its readable structure without injecting markup.
export function readableMath(value) {
  const subscripts = "₀₁₂₃₄₅₆₇₈₉";
  return String(value || "").replace(/\$([^$\n]+)\$/g, (_, expression) => expression
    .replace(/\\(?:leq?|le)/g, "≤")
    .replace(/\\(?:geq?|ge)/g, "≥")
    .replace(/\\times/g, "×")
    .replace(/\\cdot/g, "·")
    .replace(/\\(max|min|log|sqrt|sum)/g, "$1")
    .replace(/\\(?:left|right)/g, "")
    .replace(/_\{?(\d+)\}?/g, (_match, digits) => [...digits].map((digit) => subscripts[Number(digit)]).join("")));
}

export function problemText(value) {
  return readableMath(String(value || "")
    .replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1>/gi, "")
    .replace(/<br\s*\/?\s*>/gi, "\n")
    .replace(/<li\b[^>]*>/gi, "- ")
    .replace(/<\/li>/gi, "\n")
    .replace(/<\/?code\b[^>]*>/gi, "`")
    .replace(/<\/(?:p|div|h[1-6]|ul|ol|pre|blockquote)>/gi, "\n\n")
    .replace(/<\/?[a-z][^>]*>/gi, "")
    .replace(/<\/?[a-z][^>]*$/gi, "")
    .replace(/&(#(?:x[0-9a-f]+|\d+)|nbsp|amp|lt|gt|quot|apos|mdash|ndash);/gi, (match, entity) => {
      const named = { nbsp: " ", amp: "&", lt: "<", gt: ">", quot: '"', apos: "'", mdash: "—", ndash: "–" };
      if (!entity.startsWith("#")) return named[entity.toLowerCase()] || match;
      const number = entity[1]?.toLowerCase() === "x" ? parseInt(entity.slice(2), 16) : parseInt(entity.slice(1), 10);
      return number > 0 && number <= 0x10ffff ? String.fromCodePoint(number) : match;
    })
    .replace(/\n{3,}/g, "\n\n")
    .trim());
}

const IMAGE_HOSTS = new Set(["assets.leetcode.com", "leetcode.com", "fastly.jsdelivr.net", "pic.leetcode-cn.com"]);

export function problemImages(value) {
  const images = [];
  for (const tag of String(value || "").match(/<img\b[^>]*>/gi) || []) {
    const raw = tag.match(/\bsrc\s*=\s*(["'])(.*?)\1/i)?.[2];
    if (!raw) continue;
    try {
      const url = new URL(raw);
      if (url.protocol === "https:" && IMAGE_HOSTS.has(url.hostname)) {
        images.push({ src: url.href, alt: problemText(tag.match(/\balt\s*=\s*(["'])(.*?)\1/i)?.[2] || "Problem illustration") });
      }
    } catch { /* Ignore invalid image URLs from imported descriptions. */ }
  }
  return images;
}
