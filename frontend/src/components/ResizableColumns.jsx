import { Children, useEffect, useId, useRef, useState } from "react";
import "./resizable-columns.css";

export default function ResizableColumns({
  as: Container = "div",
  children,
  className = "",
  initialPrimarySize = "1fr",
  initialSecondarySize = "1fr",
  minPrimaryWidth = 240,
  minSecondaryWidth = 280,
  resizeLabel = "Đổi độ rộng hai cột",
  style,
  ...props
}) {
  const [primaryShare, setPrimaryShare] = useState(null);
  const [range, setRange] = useState({ min: 0, max: 100, share: 50 });
  const containerRef = useRef(null);
  const handleRef = useRef(null);
  const dragRef = useRef(null);
  const hintId = useId();
  const panes = Children.toArray(children);

  const measure = () => {
    const container = containerRef.current;
    if (!container || !handleRef.current?.getClientRects().length) return null;
    const primary = container.firstElementChild.getBoundingClientRect();
    const secondary = container.lastElementChild.getBoundingClientRect();
    const width = primary.width + secondary.width;
    if (width <= 0) return null;
    const scale = Math.min(1, width / (minPrimaryWidth + minSecondaryWidth));
    return {
      width,
      share: (primary.width / width) * 100,
      min: (minPrimaryWidth * scale / width) * 100,
      max: 100 - (minSecondaryWidth * scale / width) * 100,
    };
  };

  const clamp = (value, bounds) => Math.min(bounds.max, Math.max(bounds.min, value));

  const endDrag = (event) => {
    const drag = dragRef.current;
    if (!drag || (event?.pointerId != null && event.pointerId !== drag.pointerId)) return;
    dragRef.current = null;
    document.body.classList.remove("is-resizing-columns");
    handleRef.current?.classList.remove("is-dragging");
    if (handleRef.current?.hasPointerCapture(drag.pointerId)) {
      handleRef.current.releasePointerCapture(drag.pointerId);
    }
  };

  useEffect(() => () => {
    if (dragRef.current) document.body.classList.remove("is-resizing-columns");
  }, []);

  useEffect(() => {
    const observer = new ResizeObserver(() => {
      const container = containerRef.current;
      const handle = handleRef.current;
      if (!container || !handle?.getClientRects().length) return;
      const width = container.firstElementChild.getBoundingClientRect().width
        + container.lastElementChild.getBoundingClientRect().width;
      if (width <= 0) return;
      const scale = Math.min(1, width / (minPrimaryWidth + minSecondaryWidth));
      const min = (minPrimaryWidth * scale / width) * 100;
      const max = 100 - (minSecondaryWidth * scale / width) * 100;
      const share = (container.firstElementChild.getBoundingClientRect().width / width) * 100;
      setRange({ min, max, share });
      setPrimaryShare((current) => current == null ? current : Math.min(max, Math.max(min, current)));
    });
    observer.observe(containerRef.current);
    observer.observe(containerRef.current.firstElementChild);
    observer.observe(containerRef.current.lastElementChild);
    return () => observer.disconnect();
  }, [minPrimaryWidth, minSecondaryWidth]);

  const handlePointerDown = (event) => {
    if (!event.isPrimary || event.button !== 0) return;
    const bounds = measure();
    if (!bounds) return;
    event.preventDefault();
    event.currentTarget.focus({ preventScroll: true });
    dragRef.current = { ...bounds, x: event.clientX, pointerId: event.pointerId, previous: primaryShare };
    event.currentTarget.setPointerCapture(event.pointerId);
    event.currentTarget.classList.add("is-dragging");
    document.body.classList.add("is-resizing-columns");
  };

  const handlePointerMove = (event) => {
    const drag = dragRef.current;
    if (!drag || drag.pointerId !== event.pointerId) return;
    const next = drag.share + ((event.clientX - drag.x) / drag.width) * 100;
    setPrimaryShare(clamp(next, drag));
  };

  const handleKeyDown = (event) => {
    if (event.key === "Escape" && dragRef.current) {
      setPrimaryShare(dragRef.current.previous);
      endDrag();
      return;
    }
    const bounds = measure();
    if (!bounds) return;
    const step = event.shiftKey ? 5 : 2;
    const values = {
      ArrowLeft: bounds.share - step,
      ArrowRight: bounds.share + step,
      Home: bounds.min,
      End: bounds.max,
    };
    if (event.key in values) {
      event.preventDefault();
      setPrimaryShare(clamp(values[event.key], bounds));
    }
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      setPrimaryShare(null);
    }
  };

  return (
    <Container
      {...props}
      ref={containerRef}
      className={`resizable-columns ${className}`.trim()}
      style={{
        ...style,
        "--resizable-primary": primaryShare == null ? initialPrimarySize : `${primaryShare}fr`,
        "--resizable-secondary": primaryShare == null ? initialSecondarySize : `${100 - primaryShare}fr`,
      }}
    >
      {panes[0]}
      <div className="resizable-column-divider">
        <button
          ref={handleRef}
          className="resizable-column-handle"
          type="button"
          role="separator"
          aria-label={resizeLabel}
          aria-describedby={hintId}
          aria-orientation="vertical"
          aria-valuemin={Math.floor(range.min)}
          aria-valuemax={Math.ceil(range.max)}
          aria-valuenow={Math.round(primaryShare ?? range.share)}
          title={`${resizeLabel} · Kéo ↔; nhấp đúp để đặt lại`}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={endDrag}
          onPointerCancel={endDrag}
          onLostPointerCapture={endDrag}
          onBlur={() => endDrag()}
          onKeyDown={handleKeyDown}
          onDoubleClick={() => setPrimaryShare(null)}
        >
          <svg aria-hidden="true" viewBox="0 0 24 24" fill="none">
            <path d="M4 12h16M8 8l-4 4 4 4m8-8 4 4-4 4" />
          </svg>
        </button>
        <span id={hintId} className="sr-only">
          Kéo sang trái hoặc phải. Phím mũi tên để chỉnh, Shift để chỉnh nhanh,
          Enter hoặc nhấp đúp để đặt lại. Escape để hủy thao tác kéo.
        </span>
      </div>
      {panes[1]}
    </Container>
  );
}
