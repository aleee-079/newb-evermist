$input v_color0, v_color1, v_fog, v_refl, v_texcoord0, v_lightmapUV, v_extra

#include <bgfx_shader.sh>
#include <newb/main.sh>

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_SeasonsTexture);
SAMPLER2D_AUTOREG(s_LightMapTexture);

void main() {
  #if defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY) || defined(INSTANCING)
    gl_FragColor = vec4(1.0,1.0,1.0,1.0);
    return;
  #endif

  vec4 diffuse = texture2D(s_MatTexture, v_texcoord0);
  vec4 color = v_color0;

  #ifdef ALPHA_TEST
    if (diffuse.a < 0.6) {
      discard;
    }
  #endif

  #if defined(SEASONS) && (defined(OPAQUE) || defined(ALPHA_TEST))
    diffuse.rgb *= mix(vec3(1.0,1.0,1.0), texture2D(s_SeasonsTexture, v_color1.xy).rgb * 2.0, v_color1.z);
  #endif

  vec3 glow = nlGlow(s_MatTexture, v_texcoord0, v_extra.a);

  diffuse.rgb *= diffuse.rgb;

  vec3 waterPixelHighlight = vec3(0.0, 0.0, 0.0);

  #if defined(TRANSPARENT) && !(defined(SEASONS) || defined(RENDER_AS_BILLBOARDS))
    if (v_extra.b > 0.9) {
      vec4 diff2 = texture2D(s_MatTexture, v_texcoord0);
      float dif2 = (diff2.r + diff2.g + diff2.b)/3.0;
      
      float hl = smoothstep(0.55, 1.0, dif2);
      
      diffuse.rgb = vec3_splat(1.0 - NL_WATER_TEX_OPACITY*(1.0 - diffuse.b*1.8));
      diffuse.rgb *= v_fog.rgb * 1.8;
      diffuse.a = color.a;
      
      vec3 waterHighlightColor = vec3(0.85, 0.88, 0.92);
      float hlIntensity = mix(0.01, 3.5, clamp(v_color1.g * 2.0, 0.0, 1.0));
      
      waterPixelHighlight = waterHighlightColor * hl * hlIntensity;
      diffuse.a += mix(0.0, 0.5, clamp(v_color1.g * 2.0, 0.0, 1.0)) * hl;
    }
  #else
    diffuse.a = 1.0;
  #endif

  diffuse.rgb *= color.rgb;

  // --- CÓDIGO DE SOMBRAS INTEGRADO ---
  if (v_color1.a != 0.0) {
    if (v_color1.g < 0.64) {
        diffuse.rgb *= vec3(0.96,0.96,0.96);
    }
    if (v_color1.g < 0.639) {
        diffuse.rgb *= vec3(0.95,0.95,0.95);
    }
    if (v_color1.g < 0.638) {
        diffuse.rgb *= vec3(0.95,0.95,0.95);
    }
    if (v_color1.g < 0.637) {
        diffuse.rgb *= vec3(0.95,0.95,0.95);
    }
    if (v_color1.g < 0.636) {
        diffuse.rgb *= vec3(0.95,0.95,0.95);
    }
    if (v_color1.g < 0.635) {
        diffuse.rgb *= vec3(0.89,0.89,0.89);
    }
    if (v_color1.g < 0.634) {
        diffuse.rgb *= vec3(0.89,0.89,0.89);
    }
    if (v_color1.g < 0.633) {
        diffuse.rgb *= vec3(0.883,0.883,0.883);
    }
    if (v_color1.g < 0.632) {
        diffuse.rgb *= vec3(0.88,0.88,0.88);
    }
    if (v_color1.g < 0.631) {
        diffuse.rgb *= vec3(0.88,0.88,0.88); 
    }   
    if (v_color1.g < 0.63) {
        diffuse.rgb *= vec3(0.887,0.887,0.887); 
    }    
  }

  if (v_color1.a == 0.0) {
    diffuse.rgb *= 1.55;
    diffuse.rgb *= v_color1.g * 1.3;
  }

  float c = v_color1.r;
  if (v_color1.a <= 0.1) { 
    c = v_color1.g * 1.999; 
  }
  if (c < 0.638) { 
    diffuse.rgb *= 0.9; 
  }
  // -----------------------------------

  diffuse.rgb += glow;
  diffuse.rgb += waterPixelHighlight;

  // --- Water Foam Detection ---
  float dy_w = abs(dFdy(v_extra.g));
  float lmGrad = length(vec2(dFdx(v_lightmapUV.y), dFdy(v_lightmapUV.y)));

  bool isOneBlockUnderwater = (v_extra.g <= 62.99 && v_extra.g > 62.0) 
      && v_extra.b < 0.9
      && !(dy_w < 0.0002)
      && lmGrad < 0.01
      && step(0.85, v_lightmapUV.y) * step(v_lightmapUV.y, 0.927) == 1.0;

  if (isOneBlockUnderwater) {
    vec2 watpos = v_extra.rg * 16.0;
    float foam = fract(238.084 * sin(dot(floor(watpos), vec2(1.32, 141.3))));
    foam = smoothstep(0.2, 0.8, foam);
    diffuse.rgb = mix(diffuse.rgb, vec3(1.0, 1.0, 1.3) * 2.5, foam * 0.5);
  }
  // --------------------------------------------------

  if (v_extra.b > 0.9) {
    diffuse.rgb += v_refl.rgb*v_refl.a;
  } else if (v_refl.a > 0.0) {
    // reflective effect - only on xz plane
    float dy = abs(dFdy(v_extra.g));
    if (dy < 0.0002) {
      float mask = v_refl.a*(clamp(v_extra.r*10.0,8.2,8.8)-7.8);
      diffuse.rgb *= 1.0 - 0.6*mask;
      diffuse.rgb += v_refl.rgb*mask;
    }
  }

  diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);

  diffuse.rgb = colorCorrection(diffuse.rgb);

  gl_FragColor = diffuse;
}
