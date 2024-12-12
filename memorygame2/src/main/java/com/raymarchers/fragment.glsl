#version 330 core

in vec2 fragCord;
out vec4 color;
uniform float time;

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

// Helper function to smoothly combine two SurfaceInfos
SurfaceInfo smoothMinSurface(SurfaceInfo a, SurfaceInfo b, float k) {
    float h = clamp(0.5 + 0.5 * (b.distance - a.distance) / k, 0.0, 1.0);
    SurfaceInfo result;
    result.distance = mix(b.distance, a.distance, h) - k * h * (1.0 - h);
    result.color = mix(b.color, a.color, h);
    result.reflectivity = mix(b.reflectivity, a.reflectivity, h);
    return result;
}

SurfaceInfo map(vec3 p, int bouncedTimes) {
    float k = .7;
    if (bouncedTimes == 0) {
        k = .7;
    }

    // Initialize the result with the plane
    SurfaceInfo result;
    float planeY = -1.0;
    result.distance = sdfPlane(p, planeY);
    result.color = vec3(0.6, 0.6, 0.6);
    result.reflectivity = .8;

    const int NUM_SPHERES = 3;
    vec3 spherePositions[NUM_SPHERES];
    float sphereSizes[NUM_SPHERES];
    vec3 sphereColors[NUM_SPHERES];
    float sphereReflectivities[NUM_SPHERES];

    // Define spheres
    spherePositions[0] = vec3(1.0, 0.0, 0.5);
    sphereSizes[0] = 0.5;
    sphereColors[0] = vec3(1.0, 0.0, 0.0);
    sphereReflectivities[0] = 0.25;

    spherePositions[1] = vec3(-1.0, 0.0, 1.0);
    sphereSizes[1] = 0.5;
    sphereColors[1] = vec3(0.0, 1.0, 0.0);
    sphereReflectivities[1] = 0.25;

    spherePositions[2] = vec3(1.0, 0.0, 1.5);
    sphereSizes[2] = 0.5;
    sphereColors[2] = vec3(0.5, 0.5, 0.8);
    sphereReflectivities[2] = 0.8;

    // Loop through spheres and combine using smoothmin
    for (int i = 0; i < NUM_SPHERES; i++) {
        vec3 spherePos = spherePositions[i];
        spherePos.x += sin(time + float(i)); // Animate sphere positions
        float sdf = sdfSphere(p - spherePos, sphereSizes[i]);

        SurfaceInfo sphereInfo;
        sphereInfo.distance = sdf;
        sphereInfo.color = sphereColors[i];
        sphereInfo.reflectivity = sphereReflectivities[i];

        result = smoothMinSurface(result, sphereInfo, k);
    }

    // Define cube
    vec3 cubeSize = vec3(5.0, 1.0, 1.0);
    vec3 q1 = p + vec3(0.0, -0.5, -4.0);
    q1 *= rotationMatrix(vec3(1.0, 0.0, 0.0), time / 2.0);
    float cubeDist = sdfCube(q1, cubeSize);

    SurfaceInfo cubeInfo;
    cubeInfo.distance = cubeDist;
    cubeInfo.color = vec3(0.0, 0.0, 1.0);
    cubeInfo.reflectivity = 0.7;
    
    // Second cube
    vec3 cubeSize2 = vec3(100., .5, 100.);
    vec3 q2 = p + vec3(0.0, -3., 4.);

    float cubeDist2 = sdfCube(q2, cubeSize2);

    SurfaceInfo cubeInfo2;
    cubeInfo2.distance = cubeDist2;
    cubeInfo2.color = vec3(1.000,0.667,0.667);
    cubeInfo2.reflectivity = .95;

    // Combine cube with result
    result = smoothMinSurface(result, cubeInfo, 0.);

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

    const int bounceTimes = 32;
    const int maxMarches = 80;

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
