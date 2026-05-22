uniform sampler2D DiffuseSampler;
uniform vec2 Center;
uniform float Radius;
uniform float Thickness;
uniform float Force;
uniform float AspectRatio;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    vec2 tc = uv;

    // Correct coordinates for aspect ratio to ensure mathematically circular shockwaves
    vec2 aspectCenter = vec2(Center.x * AspectRatio, Center.y);
    vec2 aspectUV = vec2(uv.x * AspectRatio, uv.y);

    float dist = distance(aspectUV, aspectCenter);

    // Apply distortion only within the shockwave ring width
    if (dist > Radius - Thickness && dist < Radius + Thickness) {
        float diff = dist - Radius;
        float powDiff = 1.0 - pow(abs(diff / Thickness), 2.0); // smooth bell-curve intensity peak
        
        vec2 dir = normalize(aspectUV - aspectCenter);
        vec2 offset = dir * powDiff * Force;

        // Apply distortion to base coordinate
        vec2 tcDistorted = tc - vec2(offset.x / AspectRatio, offset.y);

        // Apply light dispersion (chromatic aberration Red/Blue split) along the refractive edge
        float dispersion = 0.007; // subtle color splitting
        vec2 rOffset = dir * dispersion * powDiff;

        float r = texture(DiffuseSampler, tcDistorted - rOffset / vec2(AspectRatio, 1.0)).r;
        float g = texture(DiffuseSampler, tcDistorted).g;
        float b = texture(DiffuseSampler, tcDistorted + rOffset / vec2(AspectRatio, 1.0)).b;

        fragColor = vec4(r, g, b, 1.0);
    } else {
        fragColor = texture(DiffuseSampler, tc);
    }
}
