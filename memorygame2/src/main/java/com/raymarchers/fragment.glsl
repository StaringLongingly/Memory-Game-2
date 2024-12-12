#version 330 core

in vec2 fragCord;
out vec4 color;
uniform float time;
uniform int[100] chars;
uniform int arrayX;
uniform int arrayY;

// Define a struct to hold surface information
struct SurfaceInfo {
    vec3 color;
    float reflectivity;
    float distance;
};

float sdfSphere(vec3 p, float size) {
    return length(p) - size;
}

float sdfPlane(vec3 p, float planeY) {
    return p.y - planeY;
}

float sdfCube(vec3 p, vec3 b) {
    vec3 d = abs(p) - b;
    return min(max(d.x, max(d.y, d.z)), 0.0) + length(max(d, 0.0));
}

float sdfPyramid( in vec3 p, in float h )
{
    float m2 = h*h + 0.25;
    
    // symmetry
    p.xz = abs(p.xz);
    p.xz = (p.z>p.x) ? p.zx : p.xz;
    p.xz -= 0.5;
	
    // project into face plane (2D)
    vec3 q = vec3( p.z, h*p.y - 0.5*p.x, h*p.x + 0.5*p.y);
   
    float s = max(-q.x,0.0);
    float t = clamp( (q.y-0.5*p.z)/(m2+0.25), 0.0, 1.0 );
    
    float a = m2*(q.x+s)*(q.x+s) + q.y*q.y;
	float b = m2*(q.x+0.5*t)*(q.x+0.5*t) + (q.y-m2*t)*(q.y-m2*t);
    
    float d2 = min(q.y,-q.x*m2-q.y*0.5) > 0.0 ? 0.0 : min(a,b);
    
    // recover 3D and scale, and add sign
    return sqrt( (d2+q.z*q.z)/m2 ) * sign(max(q.z,-p.y));;
}

float sdfTorus( vec3 p, vec2 t )
{
    return length( vec2(length(p.xz)-t.x,p.y) )-t.y;
}

float randomSDF(vec3 p, float seed) {
    const float size = .3;
    const int SDFcount = 3;
    seed *= float(SDFcount);
    seed = floor(seed);

    if (seed == 0) { return sdfCube(p, vec3(size)); }
    if (seed == 1) {
        p /= (size * 2.);
        vec3 q = p;
        q.y *= -1.;
        return min(sdfPyramid(p, .9), sdfPyramid(q, .9)) * (size * 2.);
    } 
    if (seed == 2) { return sdfTorus(p, vec2(size, size * .6)); } 
}

mat3 rotationMatrix(vec3 axis, float angle) {
    axis = normalize(axis);
    float s = sin(angle);
    float c = cos(angle);
    float oc = 1.0 - c;

    return mat3(
        oc * axis.x * axis.x + c,           oc * axis.x * axis.y - axis.z * s,  oc * axis.x * axis.z + axis.y * s,
        oc * axis.y * axis.x + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,
        oc * axis.z * axis.x - axis.y * s,  oc * axis.z * axis.y + axis.x * s,  oc * axis.z * axis.z + c
    );
}

float smoothmin(float d1, float d2, float k) {
    float h = clamp(0.5 + 0.5 * (d2 - d1) / k, 0.0, 1.0);
    return mix(d2, d1, h) - k * h * (1.0 - h);
}

// Random number generator
float random(vec2 co) {
    highp float a = 12.9898;
    highp float b = 78.233;
    highp float c = 43758.5453;
    highp float dt = dot(co.xy, vec2(a, b));
    highp float sn = mod(dt, 3.14);
    return fract(sin(sn) * c);
}
vec3 anyOrthogonalVector(vec3 n) {
    // Find a vector that is orthogonal to n
    if (abs(n.x) > abs(n.z)) {
        return vec3(-n.y, n.x, 0.0);
    } else {
        return vec3(0.0, -n.z, n.y);
    }
}

// Function to sample a random direction in a hemisphere oriented around a normal vector
vec3 sampleHemisphereCosine(vec3 normal, vec2 rand) {
    // Convert random numbers to spherical coordinates
    float phi = 2.0 * 3.14159265 * rand.x;
    float cosTheta = sqrt(1.0 - rand.y);
    float sinTheta = sqrt(rand.y);

    // Compute the direction in tangent space
    vec3 tangent = normalize(anyOrthogonalVector(normal));
    vec3 bitangent = cross(normal, tangent);
    vec3 direction = sinTheta * cos(phi) * tangent + sinTheta * sin(phi) * bitangent + cosTheta * normal;

    return normalize(direction);
}

// Helper function to smoothly combine two SurfaceInfos
SurfaceInfo smoothMinSurface(SurfaceInfo a, SurfaceInfo b, float k) {
    float h = clamp(0.5 + 0.5 * (b.distance - a.distance) / k, 0.0, 1.0);
    SurfaceInfo result;
    result.distance = mix(b.distance, a.distance, h) - k * h * (1.0 - h);
    result.color = mix(b.color, a.color, h);
    result.reflectivity = mix(b.reflectivity, a.reflectivity, h);
    return result;
}

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

SurfaceInfo map(vec3 p, int bouncedTimes) {
    float smoothing = 0.1;

    // Initialize the result with the plane
    SurfaceInfo result;
    vec3 cubePosition = vec3(0., 0., 3. + abs(sin(time / 10.)));
    vec3 cubeSize = vec3(5., 5., .5);
    result.distance = sdfCube(p - cubePosition, cubeSize);
    result.color = vec3(0.0314, 0.0314, 0.0314);
    result.reflectivity = .9;

    int k = 0;
    const int randomnessPasses = 3;
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            SurfaceInfo newShape;
            newShape.reflectivity = .3;
            float x = float(j) - float(4) / 2.0 + 0.5;
            float y = float(i) - float(4) / 2.0 + 0.5;
            vec3 position = vec3(vec2(x, y) * 1.2 , 0.);

            float r = random(vec2(chars[k]));
            vec3 q = p;
            q -= position;
            q *= rotationMatrix(sampleHemisphereCosine(vec3(1.0, 0.0, 0.0), vec2(r)), time * (r * 2. + .5));

            newShape.distance = randomSDF(q, r);
            newShape.color = hsv2rgb(vec3(r, .8, .8));

            result = smoothMinSurface(result, newShape, smoothing);
            k++;
        }
    }
    return result;
}

// Function to compute the normal at point p
vec3 computeNormal(vec3 p) {
    const float eps = 0.0001;
    float dx = map(p + vec3(eps, 0.0, 0.0), 0).distance - map(p - vec3(eps, 0.0, 0.0), 0).distance;
    float dy = map(p + vec3(0.0, eps, 0.0), 0).distance - map(p - vec3(0.0, eps, 0.0), 0).distance;
    float dz = map(p + vec3(0.0, 0.0, eps), 0).distance - map(p - vec3(0.0, 0.0, eps), 0).distance;
    return normalize(vec3(dx, dy, dz));
}

void main() {
    vec2 uv = fragCord;

    vec3 ro = vec3(0.0, 0.0, -3.0);
    vec3 rd = normalize(vec3(uv, 1.0));
    vec3 accumulatedColor = vec3(0.0);
    float attenuation = 1.0;

    int i;

    const int bounceTimes = 3;
    const int maxMarches = 160;

    // Bounce Loop
    for (int j = 0; j < bounceTimes; j++) {
        float t = 0.0;

        // Ray marching
        for (i = 0; i < maxMarches; i++) {
            vec3 p = ro + rd * t;
            SurfaceInfo a = map(p, j);

            float d = a.distance;
            if (d < 0.001) {
                vec3 surfaceColor = a.color;
                float reflectivity = a.reflectivity;

                // Add the surface color to the accumulated color
                accumulatedColor += attenuation * surfaceColor * (1.0 - reflectivity);

                // Update attenuation
                attenuation *= reflectivity;

                if (attenuation < 0.01) {
                    // If attenuation is too small, exit the loop
                    break;
                }

                // Compute normal and reflect the ray
                vec3 normal = computeNormal(p);
                ro = p + normal * 0.001; // Offset to prevent self-intersection
                rd = reflect(rd, normal);

                // Proceed to the next bounce
                break;
            }

            t += d;
        }

        if (i >= maxMarches) {
            // Ray did not hit anything; add background color
            vec3 backgroundColor = vec3(0.2, 0.2, 0.2);
            accumulatedColor += attenuation * backgroundColor;
            break;
        }
    }

    color = vec4(accumulatedColor, 1.0);
}
