import { useLayoutEffect, useRef } from "react";
import { animate, scroll } from "motion";
import "./landing.css";

const courseCards = [
  { title: "Thuật toán ABC", label: "Course page 1" },
  { title: "Thuật toán ABC", label: "Course page 2" },
  { title: "Thuật toán ABC", label: "Course page 3" },
];

const PARALLAX_OFFSETS = ["start end", "end start"];

function useParallax(ref, { from, to, targetRef = ref }) {
  useLayoutEffect(() => {
    const element = ref.current;
    const target = targetRef.current;

    if (!element || !target || window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      return undefined;
    }

    const animation = animate(element, { y: [from, to] }, { ease: "linear" });
    const stopScroll = scroll(animation, {
      target,
      offset: PARALLAX_OFFSETS,
    });

    return () => {
      stopScroll?.();
      animation.stop();
    };
  }, [from, ref, targetRef, to]);
}

function Parallax({ as: Component = "div", className = "", from, to, targetRef, children, ...props }) {
  const ref = useRef(null);
  useParallax(ref, { from, to, targetRef });

  return (
    <Component ref={ref} className={`dusk-parallax ${className}`.trim()} {...props}>
      {children}
    </Component>
  );
}

function LandingBar() {
  return (
    <header className="dusk-nav" aria-label="Main navigation">
      <a className="dusk-brand" href="#top" aria-label="AlgoViz home">
        AlgoViz
      </a>
      <nav className="dusk-auth" aria-label="Account navigation">
        <a className="dusk-nav-link" href="#login">
          Login
        </a>
        <a className="dusk-register" href="#register">
          Register
        </a>
      </nav>
    </header>
  );
}

function HeroSection() {
  const sectionRef = useRef(null);

  return (
    <section ref={sectionRef} className="dusk-hero" id="top" aria-labelledby="hero-title">
      <Parallax
        as="img"
        className="dusk-hero-image"
        targetRef={sectionRef}
        from={-22}
        to={22}
        src="/assets/algoviz-hero.svg"
        alt="Student learning algorithms at a desk"
        loading="eager"
        decoding="async"
      />
      <div className="dusk-hero-vignette" aria-hidden="true" />
      <Parallax
        className="dusk-hero-content"
        targetRef={sectionRef}
        from={26}
        to={-20}
      >
        <h1 id="hero-title" className="type-display dusk-hero-title">
          See It.
          <br />
          Understand It.
          <br />
          Master It.
        </h1>
        <p className="type-body dusk-hero-subtitle">
          Less Trial and Error, More Precise Solutions.
        </p>
        <a className="dusk-cta dusk-cta-primary" href="#register">
          Try now!
        </a>
      </Parallax>
    </section>
  );
}

function AlgorithmIntro() {
  const sectionRef = useRef(null);

  return (
    <section
      ref={sectionRef}
      className="dusk-intro"
      id="algorithms"
      aria-labelledby="algorithm-title"
    >
      <Parallax
        as="figure"
        className="dusk-illustration-frame"
        targetRef={sectionRef}
        from={-16}
        to={16}
      >
        <img
          src="/assets/algoviz-algorithm.png"
          alt="Illustration asking what is an algorithm"
          loading="lazy"
          decoding="async"
        />
      </Parallax>
      <Parallax
        className="dusk-intro-copy"
        targetRef={sectionRef}
        from={18}
        to={-14}
      >
        <h2 id="algorithm-title" className="type-display dusk-section-title">
          What is an Algorithm?
        </h2>
        <p className="type-body dusk-section-description">
          An algorithm is a finite sequence of <strong>well-defined steps or instructions</strong> used to solve <strong>a problem or accomplish</strong> a specific task.
        </p>
        <div className="dusk-actions">
          <a className="dusk-cta dusk-cta-sage" href="#register">
            Types of Algorithms
          </a>
          <a className="dusk-cta dusk-cta-secondary" href="#all">
            View all
          </a>
        </div>
      </Parallax>
    </section>
  );
}

function CourseCard({ title, label, index, sectionRef }) {
  const parallaxOffset = index === 1 ? 14 : index === 2 ? -12 : 8;

  return (
    <Parallax
      className={`dusk-card-parallax dusk-card-parallax--${index}`}
      targetRef={sectionRef}
      from={parallaxOffset}
      to={-parallaxOffset}
    >
      <article className="dusk-course-card">
        <div className="dusk-course-image" aria-hidden="true" />
        <div className="dusk-course-copy">
          <h3 className="type-body">{title}</h3>
        </div>
        <a className="dusk-course-link" href="#all" aria-label={label}>
          <span aria-hidden="true">→</span>
        </a>
      </article>
    </Parallax>
  );
}

function CoursesSection() {
  const sectionRef = useRef(null);

  return (
    <section ref={sectionRef} className="dusk-courses" id="types" aria-label="Algorithm courses">
      <div className="dusk-course-grid">
        {courseCards.map((course, index) => (
          <CourseCard
            key={course.label}
            index={index}
            sectionRef={sectionRef}
            {...course}
          />
        ))}
      </div>
      <Parallax
        className="dusk-pagination"
        targetRef={sectionRef}
        from={10}
        to={-10}
        aria-label="Course pages"
      >
        <a className="dusk-dot is-active" href="#types" aria-label="Current page" aria-current="page" />
        <a className="dusk-dot" href="#types" aria-label="Page 2" />
        <a className="dusk-dot" href="#types" aria-label="Page 3" />
      </Parallax>
    </section>
  );
}

function Landing() {
  return (
    <main className="landing-page">
      <div className="dusk-shell">
        <LandingBar />
        <HeroSection />
        <AlgorithmIntro />
        <CoursesSection />
      </div>
    </main>
  );
}

export default Landing;
