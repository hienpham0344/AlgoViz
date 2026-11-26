export function paginationItems(page, pageCount) {
  const last = Math.max(1, pageCount);
  const current = Math.min(last, Math.max(1, page));
  const pages = [...new Set([1, last, current - 1, current, current + 1]
    .filter((number) => number >= 1 && number <= last))].sort((a, b) => a - b);
  return pages.flatMap((number, index) =>
    index > 0 && number - pages[index - 1] > 1 ? ["ellipsis", number] : [number]);
}
