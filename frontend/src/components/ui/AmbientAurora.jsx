import { useEffect, useRef, useState } from 'react';

/**
 * AmbientAurora — creates an ambient aurora light background with floating particles
 * and gentle violet/cyan illumination.
 *
 * Scoped to Hero panels and Login/Signup screens only.
 * Degrades cleanly to static CSS gradient mesh when prefers-reduced-motion is active
 * or canvas context cannot be initialized.
 */
export default function AmbientAurora({ className = '', opacity = 0.45 }) {
    const canvasRef = useRef(null);
    const [reducedMotion, setReducedMotion] = useState(false);

    useEffect(() => {
        // Check prefers-reduced-motion
        const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
        setReducedMotion(mediaQuery.matches);

        const handleMotionChange = (e) => setReducedMotion(e.matches);
        mediaQuery.addEventListener('change', handleMotionChange);

        return () => mediaQuery.removeEventListener('change', handleMotionChange);
    }, []);

    useEffect(() => {
        if (reducedMotion) return;

        const canvas = canvasRef.current;
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        let animationFrameId;
        let width = (canvas.width = canvas.offsetWidth);
        let height = (canvas.height = canvas.offsetHeight);

        const handleResize = () => {
            if (!canvas) return;
            width = canvas.width = canvas.offsetWidth;
            height = canvas.height = canvas.offsetHeight;
        };

        window.addEventListener('resize', handleResize);

        // Ambient aurora light blobs
        const orbs = [
            { x: width * 0.25, y: height * 0.3, radius: 240, vx: 0.25, vy: 0.18, color: 'rgba(124, 92, 252, 0.28)' },
            { x: width * 0.75, y: height * 0.5, radius: 280, vx: -0.22, vy: 0.24, color: 'rgba(34, 211, 238, 0.22)' },
            { x: width * 0.5, y: height * 0.8, radius: 200, vx: 0.18, vy: -0.15, color: 'rgba(168, 85, 247, 0.18)' },
        ];

        // Star-dust particles
        const particleCount = 28;
        const particles = Array.from({ length: particleCount }, () => ({
            x: Math.random() * width,
            y: Math.random() * height,
            radius: Math.random() * 1.5 + 0.5,
            alpha: Math.random() * 0.6 + 0.2,
            vx: (Math.random() - 0.5) * 0.15,
            vy: (Math.random() - 0.5) * 0.15,
        }));

        let time = 0;

        const render = () => {
            time += 0.005;
            ctx.clearRect(0, 0, width, height);

            // Draw blurred aurora orbs
            orbs.forEach((orb, i) => {
                orb.x += orb.vx + Math.sin(time + i) * 0.2;
                orb.y += orb.vy + Math.cos(time + i) * 0.2;

                if (orb.x < -100) orb.x = width + 100;
                if (orb.x > width + 100) orb.x = -100;
                if (orb.y < -100) orb.y = height + 100;
                if (orb.y > height + 100) orb.y = -100;

                const gradient = ctx.createRadialGradient(
                    orb.x, orb.y, 0,
                    orb.x, orb.y, orb.radius
                );
                gradient.addColorStop(0, orb.color);
                gradient.addColorStop(1, 'rgba(18, 16, 27, 0)');

                ctx.fillStyle = gradient;
                ctx.beginPath();
                ctx.arc(orb.x, orb.y, orb.radius, 0, Math.PI * 2);
                ctx.fill();
            });

            // Draw subtle ambient particles
            particles.forEach((p) => {
                p.x += p.vx;
                p.y += p.vy;

                if (p.x < 0) p.x = width;
                if (p.x > width) p.x = 0;
                if (p.y < 0) p.y = height;
                if (p.y > height) p.y = 0;

                ctx.fillStyle = `rgba(196, 181, 253, ${p.alpha})`;
                ctx.beginPath();
                ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
                ctx.fill();
            });

            animationFrameId = requestAnimationFrame(render);
        };

        render();

        return () => {
            window.removeEventListener('resize', handleResize);
            cancelAnimationFrame(animationFrameId);
        };
    }, [reducedMotion]);

    return (
        <div
            className={`pointer-events-none absolute inset-0 overflow-hidden ${className}`}
            style={{ opacity, zIndex: 0 }}
            aria-hidden="true"
        >
            {reducedMotion ? (
                // Static CSS Aurora Gradient Fallback
                <div
                    className="w-full h-full"
                    style={{
                        background: 'radial-gradient(ellipse at top left, rgba(124, 92, 252, 0.25) 0%, transparent 60%), radial-gradient(ellipse at bottom right, rgba(34, 211, 238, 0.2) 0%, transparent 60%)',
                    }}
                />
            ) : (
                <canvas
                    ref={canvasRef}
                    className="w-full h-full block"
                    style={{ filter: 'blur(30px)' }}
                />
            )}
        </div>
    );
}
