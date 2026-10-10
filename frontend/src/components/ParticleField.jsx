import { useEffect, useRef } from "react";
import "./particle-field.css";

const PARTICLE_COUNT = 110;
const CONNECTION_DISTANCE = 105;

function ParticleField() {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const context = canvas.getContext("2d");
    const pointer = { x: -1000, y: -1000, active: false };
    const particles = [];
    let animationFrame;
    let width = 0;
    let height = 0;

    const resize = () => {
      const pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
      width = window.innerWidth;
      height = window.innerHeight;
      canvas.width = width * pixelRatio;
      canvas.height = height * pixelRatio;
      canvas.style.width = `${width}px`;
      canvas.style.height = `${height}px`;
      context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
    };

    const createParticles = () => {
      const density = Math.min(
        PARTICLE_COUNT,
        Math.max(48, Math.floor((width * height) / 11500)),
      );
      particles.length = 0;

      for (let index = 0; index < density; index += 1) {
        particles.push({
          x: Math.random() * width,
          y: Math.random() * height,
          vx: (Math.random() - 0.5) * 0.25,
          vy: (Math.random() - 0.5) * 0.25,
          radius: Math.random() * 1.8 + 1.2,
          phase: Math.random() * Math.PI * 5,
        });
      }
    };

    const draw = (time) => {
      context.clearRect(0, 0, width, height);

      particles.forEach((particle) => {
        const pulse = 0.75 + Math.sin(time * 0.002 + particle.phase) * 0.25;
        const distanceToPointer = Math.hypot(
          particle.x - pointer.x,
          particle.y - pointer.y,
        );

        if (pointer.active && distanceToPointer < 190) {
          const pull = (1 - distanceToPointer / 190) * 0.012;
          particle.vx += (pointer.x - particle.x) * pull;
          particle.vy += (pointer.y - particle.y) * pull;
        }

        particle.vx = Math.max(-0.55, Math.min(0.55, particle.vx));
        particle.vy = Math.max(-0.55, Math.min(0.55, particle.vy));
        particle.x += particle.vx;
        particle.y += particle.vy;

        if (particle.x < -20) particle.x = width + 20;
        if (particle.x > width + 20) particle.x = -20;
        if (particle.y < -20) particle.y = height + 20;
        if (particle.y > height + 20) particle.y = -20;

        const glow = context.createRadialGradient(
          particle.x,
          particle.y,
          0,
          particle.x,
          particle.y,
          particle.radius * 24,
        );
        glow.addColorStop(0, `rgba(154, 255, 224, ${0.38 * pulse})`);
        glow.addColorStop(0.2, `rgba(78, 211, 190, ${0.2 * pulse})`);
        glow.addColorStop(0.56, `rgba(78, 211, 190, ${0.07 * pulse})`);
        glow.addColorStop(1, "rgba(78, 211, 190, 0)");
        context.fillStyle = glow;
        context.beginPath();
        context.arc(
          particle.x,
          particle.y,
          particle.radius * 24,
          0,
          Math.PI * 2,
        );
        context.fill();

        context.fillStyle = `rgba(205, 255, 240, ${0.75 + pulse * 0.25})`;
        context.beginPath();
        context.arc(particle.x, particle.y, particle.radius, 0, Math.PI * 2);
        context.fill();
      });

      particles.forEach((particle, index) => {
        for (
          let otherIndex = index + 1;
          otherIndex < particles.length;
          otherIndex += 1
        ) {
          const other = particles[otherIndex];
          const distance = Math.hypot(
            particle.x - other.x,
            particle.y - other.y,
          );

          if (distance < CONNECTION_DISTANCE) {
            const opacity = (1 - distance / CONNECTION_DISTANCE) * 0.28;
            context.strokeStyle = `rgba(93, 224, 199, ${opacity})`;
            context.lineWidth = 1;
            context.beginPath();
            context.moveTo(particle.x, particle.y);
            context.lineTo(other.x, other.y);
            context.stroke();
          }
        }
      });

      animationFrame = requestAnimationFrame(draw);
    };

    const movePointer = (event) => {
      pointer.x = event.clientX;
      pointer.y = event.clientY;
      pointer.active = true;
    };

    const leaveWindow = () => {
      pointer.active = false;
    };

    resize();
    createParticles();
    animationFrame = requestAnimationFrame(draw);
    window.addEventListener("resize", resize);
    window.addEventListener("pointermove", movePointer, { passive: true });
    window.addEventListener("blur", leaveWindow);

    return () => {
      cancelAnimationFrame(animationFrame);
      window.removeEventListener("resize", resize);
      window.removeEventListener("pointermove", movePointer);
      window.removeEventListener("blur", leaveWindow);
    };
  }, []);

  return (
    <canvas ref={canvasRef} className="particle-field" aria-hidden="true" />
  );
}

export default ParticleField;
