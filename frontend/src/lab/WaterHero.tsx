import { useEffect, useRef } from 'react';
import './water-hero.css';

/**
 * 실험용 WebGL2 물방울 글래스모피즘 히어로.
 * 호버 시 물이 맺히며 굴절/광택, 해제 시 노이즈를 따라 말라 사라진다.
 * ⚠️ lab 전용 — 디자인 시스템/토큰 규칙과 무관. 삭제해도 앱에 영향 없음.
 */

// ── 튜닝 파라미터 ──────────────────────────────────────────
const PARAMS = {
  wetInSpeed: 3.2, // 물 맺히는 속도(클수록 빠름)
  dryOutSpeed: 1.1, // 마르는 속도(작을수록 천천히)
  tiltDeg: 5, // 마우스에 따른 3D 기울기 최대 각도
};

const VERT = `#version 300 es
in vec2 aPos;
out vec2 vUv;
void main(){
  vUv = aPos * 0.5 + 0.5;
  gl_Position = vec4(aPos, 0.0, 1.0);
}`;

const FRAG = `#version 300 es
precision highp float;
uniform sampler2D uTex;
uniform float uTime, uWet;
uniform vec2 uMouse;
uniform float uAspect, uImgAspect;
in vec2 vUv;
out vec4 frag;

float hash21(vec2 p){ p=fract(p*vec2(123.34,456.21)); p+=dot(p,p+45.32); return fract(p.x*p.y); }
float noise(vec2 p){
  vec2 i=floor(p), f=fract(p);
  float a=hash21(i), b=hash21(i+vec2(1,0)), c=hash21(i+vec2(0,1)), d=hash21(i+vec2(1,1));
  vec2 u=f*f*(3.0-2.0*f);
  return mix(mix(a,b,u.x), mix(c,d,u.x), u.y);
}
float fbm(vec2 p){ float s=0.0, a=0.5; for(int i=0;i<5;i++){ s+=a*noise(p); p=p*2.0+7.0; a*=0.5; } return s; }

// 매끄러운 물막(물방울 없음): 큰 파동으로 은은한 굴절만
float membrane(vec2 uv, float t){
  float a = sin(uv.x*5.0 + t*0.7) + sin(uv.y*4.3 - t*0.55);
  float b = sin((uv.x*3.1 + uv.y*3.7) + t*0.45);
  return (a + b) * 0.25;
}

void main(){
  float ca = uAspect, ia = uImgAspect;
  vec2 scale = ca > ia ? vec2(1.0, ia/ca) : vec2(ca/ia, 1.0);
  vec2 baseUv = (vUv-0.5)*scale + 0.5;

  // 마름 마스크: 격자 없는 부드러운 사인 필드. uWet 이 줄면 물결처럼 패치로 물러남
  float dryN = 0.5
    + 0.22*sin(vUv.x*5.2 + 1.0) + 0.22*sin(vUv.y*4.4 + 2.3)
    + 0.16*sin((vUv.x*3.0 + vUv.y*3.6) + 4.0);
  float mask = clamp(smoothstep(dryN-0.30, dryN+0.02, uWet*1.15), 0.0, 1.0);

  // 매끄러운 법선(방울 없음)
  float e = 0.004;
  vec2 grad = vec2(
    membrane(vUv+vec2(e,0.0), uTime) - membrane(vUv-vec2(e,0.0), uTime),
    membrane(vUv+vec2(0.0,e), uTime) - membrane(vUv-vec2(0.0,e), uTime)
  );
  vec3 nrm = normalize(vec3(grad * 7.0, 1.0));
  nrm = normalize(mix(vec3(0.0,0.0,1.0), nrm, mask));

  vec3 dry = texture(uTex, baseUv).rgb;
  vec2 ruv = baseUv + nrm.xy * 0.02 * mask;   // 은은한 굴절
  vec3 col = texture(uTex, ruv).rgb;

  // 뿌연 유리막(frosted): 살짝 밝고 채도↓
  vec3 lum = vec3(dot(col, vec3(0.299,0.587,0.114)));
  vec3 film = mix(col, lum, 0.10);
  film = film*1.03 + 0.05;

  // 광택/프레넬 — 광원은 항상 비스듬(고정 baseline), 마우스는 살짝만 흔든다.
  // (마우스가 중앙이면 L 이 평평한 법선과 평행 → 전체 whiteout 되던 문제 방지)
  vec3 L = normalize(vec3(0.35 + (uMouse.x-0.5)*0.3, 0.40 + (uMouse.y-0.5)*0.3, 0.85));
  float spec = pow(max(dot(nrm, L), 0.0), 60.0);  // 좁은 글린트 → 평평한 면은 거의 0
  float fres = pow(1.0 - nrm.z, 2.5);

  // 흐르는 sheen 밴드
  float sheen = smoothstep(0.02, 0.0, abs(fract(vUv.x*0.6 - vUv.y*0.3 - uTime*0.06)-0.5)-0.02);

  vec3 wet = film + spec*0.6*mask + fres*0.2*mask + sheen*0.08*mask;

  frag = vec4(mix(dry, wet, mask), 1.0);
}`;

function compile(gl: WebGL2RenderingContext, type: number, src: string) {
  const sh = gl.createShader(type)!;
  gl.shaderSource(sh, src);
  gl.compileShader(sh);
  if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) {
    console.error('[WaterHero] shader:', gl.getShaderInfoLog(sh));
    gl.deleteShader(sh);
    return null;
  }
  return sh;
}

export function WaterHero({
  src,
  alt = '',
  className,
}: {
  src: string;
  alt?: string;
  className?: string;
}) {
  const rootRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const root = rootRef.current!;
    const canvas = canvasRef.current!;
    const gl = canvas.getContext('webgl2', { premultipliedAlpha: false, antialias: true });
    if (!gl) return; // 미지원 → 아래 <img> 폴백 그대로

    const program = gl.createProgram()!;
    const vs = compile(gl, gl.VERTEX_SHADER, VERT);
    const fs = compile(gl, gl.FRAGMENT_SHADER, FRAG);
    if (!vs || !fs) return;
    gl.attachShader(program, vs);
    gl.attachShader(program, fs);
    gl.linkProgram(program);
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
      console.error('[WaterHero] link:', gl.getProgramInfoLog(program));
      return;
    }
    gl.useProgram(program);

    // 풀스크린 삼각형
    const buf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buf);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
    const aPos = gl.getAttribLocation(program, 'aPos');
    gl.enableVertexAttribArray(aPos);
    gl.vertexAttribPointer(aPos, 2, gl.FLOAT, false, 0, 0);

    const u = {
      tex: gl.getUniformLocation(program, 'uTex'),
      time: gl.getUniformLocation(program, 'uTime'),
      wet: gl.getUniformLocation(program, 'uWet'),
      mouse: gl.getUniformLocation(program, 'uMouse'),
      aspect: gl.getUniformLocation(program, 'uAspect'),
      imgAspect: gl.getUniformLocation(program, 'uImgAspect'),
    };

    // 텍스처
    const tex = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, tex);
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
    // 로드 전 1px 플레이스홀더
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, 1, 1, 0, gl.RGBA, gl.UNSIGNED_BYTE, new Uint8Array([200, 200, 200, 255]));
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);

    let imgAspect = 1;
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      imgAspect = img.naturalWidth / img.naturalHeight;
      gl.bindTexture(gl.TEXTURE_2D, tex);
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, img);
      draw(performance.now());
    };
    img.src = src;

    // 상태
    let wet = 0;
    let target = 0;
    let mouse = [0.5, 0.5];
    let raf = 0;
    let running = false;
    let last = performance.now();
    const start = performance.now();

    function resize() {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const w = Math.max(1, Math.round(root.clientWidth * dpr));
      const h = Math.max(1, Math.round(root.clientHeight * dpr));
      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
        gl!.viewport(0, 0, w, h);
      }
    }

    function draw(now: number) {
      const dt = Math.min((now - last) / 1000, 0.05);
      last = now;
      const speed = target > wet ? PARAMS.wetInSpeed : PARAMS.dryOutSpeed;
      wet += (target - wet) * Math.min(dt * speed, 1);
      if (Math.abs(target - wet) < 0.001) wet = target;

      resize();
      gl!.useProgram(program);
      gl!.bindTexture(gl!.TEXTURE_2D, tex);
      gl!.uniform1i(u.tex, 0);
      gl!.uniform1f(u.time, (now - start) / 1000);
      gl!.uniform1f(u.wet, wet);
      gl!.uniform2f(u.mouse, mouse[0], mouse[1]);
      gl!.uniform1f(u.aspect, canvas.width / canvas.height);
      gl!.uniform1f(u.imgAspect, imgAspect);
      gl!.drawArrays(gl!.TRIANGLES, 0, 3);

      // 물이 다 마르면 루프 정지(마지막 dry 프레임은 원본과 동일)
      if (wet <= 0.0005 && target === 0) {
        running = false;
        return;
      }
      raf = requestAnimationFrame(draw);
    }

    function ensureRunning() {
      if (!running) {
        running = true;
        last = performance.now();
        raf = requestAnimationFrame(draw);
      }
    }

    // 부유: 틸트(rx/ry) + 틸트-반응 그림자(sx/sy)를 CSS 변수로 갱신.
    // (transform 을 직접 쓰지 않고 변수만 바꿔 CSS의 lift 와 합성)
    function setFloat(rx: number, ry: number, sx: number, sy: number) {
      root.style.setProperty('--rx', `${rx}deg`);
      root.style.setProperty('--ry', `${ry}deg`);
      root.style.setProperty('--sx', `${sx}px`);
      root.style.setProperty('--sy', `${sy}px`);
    }

    // 이벤트
    function onEnter() {
      target = 1;
      root.dataset.wet = 'true';
      ensureRunning();
    }
    function onLeave() {
      target = 0;
      root.dataset.wet = 'false';
      setFloat(0, 0, 0, 0); // 안착(리프트만) 복귀
      ensureRunning();
    }
    function onMove(e: PointerEvent) {
      const r = root.getBoundingClientRect();
      const mx = (e.clientX - r.left) / r.width;
      const my = (e.clientY - r.top) / r.height;
      mouse = [mx, 1 - my]; // uv y-flip
      const t = PARAMS.tiltDeg;
      // 그림자는 틸트 반대 방향으로 이동 → 물리적으로 "판이 기운다" 느낌
      setFloat((0.5 - my) * t, (mx - 0.5) * t, (0.5 - mx) * 18, (0.5 - my) * 10);
      root.dataset.wet = 'true';
    }

    root.addEventListener('pointerenter', onEnter);
    root.addEventListener('pointerleave', onLeave);
    root.addEventListener('pointermove', onMove);

    // 테스트/디버그용: window.__waterHero.wet(0~1) 로 강제 (스크린샷 확인용)
    (window as unknown as Record<string, unknown>).__waterHero = {
      wet: (v: number) => {
        target = v;
        root.dataset.wet = v > 0 ? 'true' : 'false';
        ensureRunning();
      },
    };

    draw(performance.now());

    return () => {
      cancelAnimationFrame(raf);
      root.removeEventListener('pointerenter', onEnter);
      root.removeEventListener('pointerleave', onLeave);
      root.removeEventListener('pointermove', onMove);
      gl.deleteProgram(program);
      gl.deleteTexture(tex);
      gl.deleteBuffer(buf);
    };
  }, [src]);

  return (
    <div ref={rootRef} className={`lab-water-hero ${className ?? ''}`} data-wet="false">
      <img className="lab-water-hero__img" src={src} alt={alt} />
      <canvas ref={canvasRef} className="lab-water-hero__canvas" aria-hidden="true" />
    </div>
  );
}
