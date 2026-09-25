import os
import subprocess

svg_content = '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <defs>
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#094FE2" />
      <stop offset="45%" stop-color="#042F99" />
      <stop offset="100%" stop-color="#001458" />
    </linearGradient>
    <radialGradient id="glossGrad" cx="30%" cy="20%" r="70%">
      <stop offset="0%" stop-color="#4B95FF" stop-opacity="0.65" />
      <stop offset="50%" stop-color="#042F99" stop-opacity="0" />
      <stop offset="100%" stop-color="#000A30" stop-opacity="0.8" />
    </radialGradient>
    <linearGradient id="boothGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFFFFF" />
      <stop offset="60%" stop-color="#EDF4FF" />
      <stop offset="100%" stop-color="#B8D5FF" />
    </linearGradient>
    <linearGradient id="deskPlatformGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#00AEFF" />
      <stop offset="100%" stop-color="#005BDB" />
    </linearGradient>
    <linearGradient id="bookLeftGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#00C2FF" />
      <stop offset="100%" stop-color="#0072EA" />
    </linearGradient>
    <linearGradient id="bookRightGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#0095FF" />
      <stop offset="100%" stop-color="#0051B8" />
    </linearGradient>
    <linearGradient id="lampGlowGrad" x1="80%" y1="15%" x2="20%" y2="85%">
      <stop offset="0%" stop-color="#FFF37A" stop-opacity="0.95" />
      <stop offset="35%" stop-color="#FFA800" stop-opacity="0.75" />
      <stop offset="70%" stop-color="#FF7700" stop-opacity="0.30" />
      <stop offset="100%" stop-color="#FF5500" stop-opacity="0.0" />
    </linearGradient>
    <linearGradient id="lampConeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFFFFF" />
      <stop offset="100%" stop-color="#CCDDF8" />
    </linearGradient>
    <filter id="dropShadow" x="-10%" y="-10%" width="130%" height="130%">
      <feDropShadow dx="0" dy="8" stdDeviation="12" flood-color="#000926" flood-opacity="0.5" />
    </filter>
    <filter id="lampBloom" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="8" result="blur" />
      <feComposite in="SourceGraphic" in2="blur" operator="over" />
    </filter>
  </defs>

  <!-- Squircle Base -->
  <rect x="16" y="16" width="480" height="480" rx="108" ry="108" fill="url(#bgGrad)" />
  <rect x="16" y="16" width="480" height="480" rx="108" ry="108" fill="url(#glossGrad)" />
  
  <!-- Subtle Outer Rim Reflection -->
  <rect x="18" y="18" width="476" height="476" rx="106" ry="106" fill="none" stroke="#6AAEFF" stroke-width="3" stroke-opacity="0.4" />

  <!-- Lamp Warm Light Cone Beam (Projected Downward Left) -->
  <path d="M 335 155 L 235 240 L 195 330 L 330 330 L 360 215 Z" fill="url(#lampGlowGrad)" filter="url(#lampBloom)" />
  <ellipse cx="320" cy="180" rx="35" ry="24" fill="#FFE552" opacity="0.6" filter="url(#lampBloom)" />

  <!-- Stylized Study Cubicle / Desk L-Shape -->
  <g filter="url(#dropShadow)">
    <path d="M 125 155 C 125 130 145 110 170 110 L 180 110 C 195 110 205 122 205 137 L 205 270 C 205 295 225 315 250 315 L 365 315 C 380 315 392 327 392 342 L 392 348 C 392 373 372 393 347 393 L 175 393 C 147 393 125 371 125 343 Z" fill="url(#boothGrad)" />
  </g>

  <!-- Blue Desk Mat / Shelf Plate -->
  <path d="M 235 320 L 392 320 C 408 320 415 330 405 342 L 350 390 L 225 390 L 210 335 C 210 325 220 320 235 320 Z" fill="url(#deskPlatformGrad)" opacity="0.95" />

  <!-- Student Avatar (Head & Torso) -->
  <g filter="url(#dropShadow)">
    <!-- Head -->
    <circle cx="260" cy="210" r="32" fill="#FFFFFF" />
    <circle cx="260" cy="210" r="32" fill="url(#boothGrad)" />
    <!-- Torso / Shoulders -->
    <path d="M 200 295 C 200 252 225 242 260 242 C 295 242 320 252 320 295 C 300 300 280 305 260 305 C 240 305 220 300 200 295 Z" fill="#FFFFFF" />
  </g>

  <!-- Open 3D Study Book -->
  <g filter="url(#dropShadow)">
    <!-- Left Page -->
    <polygon points="190,285 260,312 260,378 190,345" fill="url(#bookLeftGrad)" />
    <!-- Right Page -->
    <polygon points="260,312 330,285 330,345 260,378" fill="url(#bookRightGrad)" />
    <!-- Center Crease / Spine Highlight -->
    <line x1="260" y1="312" x2="260" y2="378" stroke="#FFFFFF" stroke-width="2" stroke-opacity="0.6" />
    <!-- Book 3D Base Thickness -->
    <polygon points="190,345 260,378 330,345 330,356 260,388 190,356" fill="#00378C" />
  </g>

  <!-- Articulated Desk Lamp -->
  <g filter="url(#dropShadow)">
    <!-- Base -->
    <ellipse cx="380" cy="325" rx="14" ry="7" fill="#CCDDF8" />
    
    <!-- Arm Segment Lower -->
    <line x1="380" y1="325" x2="400" y2="225" stroke="#FFFFFF" stroke-width="12" stroke-linecap="round" />
    
    <!-- Joint Elbow 1 -->
    <circle cx="400" cy="225" r="14" fill="#FFFFFF" />
    <circle cx="400" cy="225" r="7" fill="#A4C4F4" />

    <!-- Arm Segment Upper -->
    <line x1="400" y1="225" x2="350" y2="145" stroke="#FFFFFF" stroke-width="12" stroke-linecap="round" />

    <!-- Joint Elbow 2 -->
    <circle cx="350" cy="145" r="13" fill="#FFFFFF" />
    <circle cx="350" cy="145" r="6" fill="#A4C4F4" />

    <!-- Lamp Shade / Hood (Cone) -->
    <path d="M 338 128 L 372 155 C 362 178 318 205 300 185 C 288 170 318 135 338 128 Z" fill="url(#lampConeGrad)" />
    <!-- Glowing Bulb inside lamp shade -->
    <ellipse cx="318" cy="172" rx="14" ry="10" transform="rotate(-35, 318, 172)" fill="#FFF47D" />
    <ellipse cx="318" cy="172" rx="8" ry="6" transform="rotate(-35, 318, 172)" fill="#FFFFFF" />
  </g>
</svg>
'''

with open("/tmp/icon_art.svg", "w") as f:
    f.write(svg_content)

print("SVG generated successfully at /tmp/icon_art.svg")
