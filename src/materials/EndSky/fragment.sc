#ifndef INSTANCING
$input v_texcoord0, v_posTime
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
  #include <newb/main.sh>

  SAMPLER2D_AUTOREG(s_SkyTexture);

vec3 getBH(vec2 uv, float t)
{
    // AJUSTE DE POSICIÓN:
    uv.y -= 0.17;
    uv.x += 0.0;

    // PALETA DE COLORES
    vec3  diskColor  = vec3(0.65, 0.15, 0.95);
    vec3  coreColor  = vec3(0.90, 0.40, 1.00);

    float zoom       = 0.2;
    float brightness = 2.1;
    float rightAura  = 1.1;
    float leftAura   = 0.9;

    vec2 p = uv / zoom;

    float iter   = 0.2;
    vec2 diag   = vec2(-0.30, 1.05);
    vec2 center = p - iter * diag;

    vec2 warped = p * mat2(1.0, 1.0, diag / (0.1 + iter / dot(center, center)));

    float radius = dot(warped, warped);
    float angle  = 1.5 * log(radius + 0.0001) + t * iter;

    mat2 rot = mat2(
        cos(angle), -sin(angle),
        sin(angle),  cos(angle)
    );

    vec2 spiral = rot * warped;
    spiral /= iter;

    float waveSum = 0.0;

    for(int n = 0; n < 6; n++)
    {
        iter += 1.0;
        vec2 s = sin(spiral);

        waveSum += 4.0 + 2.0 * (s.x + s.y);

        spiral += 0.7 * sin(spiral.yx * iter + t) / iter + 0.5;
    }

    float disk = length(sin(spiral) * 0.4 + warped * (3.0 + diag));

    float sideAura = mix(leftAura, rightAura, smoothstep(-0.2, 0.2, warped.x));

    float waveFactor = waveSum * 0.25;

    float denominator = (waveFactor + 0.0001)
                      * (2.0 + disk * disk * 0.25 - disk)
                      * (0.5 + 1.0 / radius)
                      * (0.03 + abs(length(p) - 0.7));

    float energy = 1.0 - exp(-exp(abs(warped.x)) / denominator);

    vec3 finalTint = mix(diskColor, coreColor, clamp(energy * 0.8, 0.0, 1.0));
    vec3 colorRGB = energy * finalTint;

    colorRGB *= sideAura;
    colorRGB *= brightness;

    return colorRGB;
}
#endif

void main() {
  #ifndef INSTANCING
    vec4 diffuse = texture2D(s_SkyTexture, v_texcoord0);

    vec3 viewDir = normalize(v_posTime.xyz);
    float t = v_posTime.w;

    
    vec2 bhUV = vec2(viewDir.x, viewDir.y) / max(viewDir.z + 1.0, 0.001);

    
    vec3 color = renderEndSky(getEndHorizonCol(), getEndZenithCol(), viewDir, t);
    color += 2.8 * diffuse.rgb;


    if (viewDir.z > -0.2) {
        vec3 bhColor = getBH(bhUV, t);
        // Atenuación suave en los bordes para evitar costuras visuales
        float fade = smoothstep(-0.2, 0.3, viewDir.z);
        color += bhColor * fade;
    }

    color = colorCorrection(color);

    gl_FragColor = vec4(color, 1.0);
  #else
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
  #endif
}
