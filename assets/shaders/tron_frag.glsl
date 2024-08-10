#version 330 core
// --- constants
#define GAMMA 2.2f
#define INV_GAMMA (1.0f / 2.2f)
#define PI 3.14159265359f
#define INV_PI (1.0f / PI)

#define MAX_POINT_LIGHTS 10
#define MAX_SPOT_LIGHTS 10

// --- Input from vertex shader
in struct VertexData
{
    vec2 textureCoordinate;
    vec3 normal;
    // Vectors needed for lighting calculations. Must not be normalized in the vertex shader.
    vec3 toCamera;
    vec3 toPointLight[MAX_POINT_LIGHTS];
    vec3 toSpotLight[MAX_SPOT_LIGHTS];
} vertexData;

// --- Materials
// Material textures
uniform sampler2D materialDiff;
uniform sampler2D materialSpec;
uniform sampler2D materialEmit;
// Material shininess parameter
uniform float materialShininess;
// Multiply with (grayscale) emmissive texture to get "glow" in different colors.
uniform vec3 shadingColor;

// --- Lights
struct PointLight
{
    vec3 Color;
    vec3 Position;
};
struct SpotLight
{
    vec3 Color;
    vec3 Position;
    vec2 Cone;
    vec3 Direction;
};
// Fixed-size uniform arrays, but with a runtime-configurable number of lights
uniform PointLight pointLight[MAX_POINT_LIGHTS];
uniform int numPointLights;
uniform SpotLight spotLight[MAX_SPOT_LIGHTS];
uniform int numSpotLights;

// --- Fragment shader output
out vec4 color;

// --- Calculates the amount of light that arrives at the shaded point from a point light
vec3 getPointLightIntensity(vec3 color, vec3 toLightVector)
{
    // distance to the light source is the length of the toLightVector
    float d = length(toLightVector);
    // incident light is light color multiplied with inverse-square-law attenuation
    return color * (1.0f / (d * d));
}

// --- Calculates the amount of light that arrives at the shaded point from a spot light
vec3 getSpotLightIntensity(vec3 color, vec3 toLightVector, vec3 lightdir, vec2 cone)
{
    // distance to the light source is the length of the toLightVector
    float d = length(toLightVector);
    // cosine of the angle between the spot light direction and the vector from spot light to shaded point
    float cosfpos = dot(lightdir, normalize(-toLightVector));
    // attenuation factor is 1 inside the inner cone, 0 outside the outer cone and a linear ramp in between
    float att = clamp((cosfpos - cos(cone.y)) / (cos(cone.x) - cos(cone.y)), 0.0f, 1.0f);
    // incident light is light color multiplied with inverse-square-law attenuation and the cone attenuation factor
    return color * att * (1.0f / (d * d));
}

// --- Phong-BRDF
// Determines what fraction of light leaves the surface point in direction V, coming from direction L,
// using the Phong reflection model.
vec3 shade(vec3 N, vec3 L, vec3 V, vec3 diffc, vec3 specc, float shn)
{
    // Reflect light vector about surface normal
    vec3 R = normalize(reflect(-L, N));
    // Cosine of the angle between surface normal and light direction (Lambert Diffuse). Clamp to zero to avoid "negative illumination".
    float NdotL = max(dot(N, L), 0.0f);
    // Cosine of the angle between reflected light vector and view vector. ''
    float RdotV = max(dot(R, V), 0.0f);
    // Add up diffuse and specular terms: Lambert Diffuse + Phong Specular.
    // The highter the shininess, the "peakier" the specular term becomes. Note that this is not a physically meaningful
    // model.
    return diffc * NdotL + specc * pow(RdotV, shn);
}

// --- Blinn-Phong-BRDF
// Determines what fraction of light leaves the surface point in direction V, coming from direction L,
// using the Blinn-Phong reflection model.
vec3 shadeBlinn(vec3 N, vec3 L, vec3 V, vec3 diffc, vec3 specc, float shn)
{
    // Halfway vector between view direction and light direction. This is the normal normal direction of the contributing
    // microfacets.
    vec3 H = normalize(V + L);
    // Cosine of the angle between surface normal and light direction (Lambert Diffuse). Clamp to zero to avoid "negative illumination".
    float NdotL = max(dot(N, L), 0.0f);
    // Cosine of the angle between halfway vector and surface normal. If H == N, the fraction of contributing microfacets
    // is largest. As H deviates from N, the fraction decays smoothly.
    float HdotN = max(dot(H, N), 0.0f);
    // Add up diffuse and specular terms: Lambert Diffuse + Blinn-Phong Specular.
    // The higher the shininess, the narrower the microfacet distribution becomes around the surface normal.
    return  diffc * NdotL + specc * pow(HdotN, shn);
}

// --- converts from linear-RGB to gamma-RGB
vec3 gammaCorrect(vec3 clinear)
{
    return pow(clinear, vec3(INV_GAMMA));
}

// --- converts from gamma-RGB to linear-RGB
vec3 invGammaCorrect(vec3 cgamma)
{
    return pow(cgamma, vec3(GAMMA));
}

void main(){
    // Sample material properties from the textures and convert to linear-RGB
    vec3 diffColor = invGammaCorrect(texture(materialDiff, vertexData.textureCoordinate).rgb);
    vec3 specColor = invGammaCorrect(texture(materialSpec, vertexData.textureCoordinate).rgb);
    vec3 emitColor = invGammaCorrect(texture(materialEmit, vertexData.textureCoordinate).rgb);

    // Gather and normalize vectors needed for lighting calculations
    vec3 N = normalize(vertexData.normal);
    vec3 V = normalize(vertexData.toCamera);

    // Initialize the color accumulator with light due to self-emission.
    vec3 emit_term = emitColor * shadingColor;
    vec3 final_color = emit_term;  // this var collects the shading from all light sources

    // Due to the linearity of light, we simply add up the contributions from all light sources in the
    // final_color variable.

    // Process all point lights
    for (int i = 0; i < numPointLights; i++) {
        // Normalized toLight vector
        vec3 Lpl = normalize(vertexData.toPointLight[i]);
        // Evaluate the BRDF at the current shading point for point light i
        //vec3 pshade = shade(N, Lpl, V, diffColor, specColor, materialShininess);  // Phong shading
        vec3 pshade = shadeBlinn(N, Lpl, V, diffColor, specColor, materialShininess*2);  // Blinn-Phong shading
        // Get the incoming light intensity including inverse-square-law attenuation
        vec3 intPointLight = getPointLightIntensity(pointLight[i].Color, vertexData.toPointLight[i]);
        // Multiply BRDF with incoming light intensity to get total color contribution for point light i
        final_color += pshade * intPointLight;
    }

    // Process all spot lights
    for (int i = 0; i < numSpotLights; i++) {
        // Normalized toLight vector
        vec3 Lsl = normalize(vertexData.toSpotLight[i]);
        // Evaluate the BRDF at the current shading point for spot light i
        //vec3 sshade = shade(N, Lsl, V, diffColor, specColor, materialShininess);  // Phong shading
        vec3 sshade = shadeBlinn(N, Lsl, V, diffColor, specColor, materialShininess*2);  // Blinn-Phong shading
        // Get the incoming light intensity including inverse-square-law attenuation and cone attenuation
        vec3 intSpotLight = getSpotLightIntensity(spotLight[i].Color, vertexData.toSpotLight[i], spotLight[i].Direction, spotLight[i].Cone);
        // Multiply BRDF with incoming light intensity to get total color contribution for spot light i
        final_color += sshade * intSpotLight;
    }

    // Convert linear-RGB to gamma-RGB to account for the monitor's non-linearity and store the final fragment color.
    color = vec4(gammaCorrect(final_color), 1.0f);
}