#version 460 core

in vec2 fragCord; //pixel coord
out vec4 color;   //pixel color
uniform float time; 
uniform float[100] chars;
uniform float arrayX;
uniform float arrayY;
uniform float mouseX;
uniform float mouseY;

// Define a struct to hold surface information
struct SurfaceInfo {
    vec3 color;
    float reflectivity;
    float distance;
};

//SDF functions provided by the master of shaders Inigo Quilez, https://iquilezles.org/
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

float sdfHorseshoe( in vec3 p, in vec2 c, in float r, in float le, vec2 w )
{
    p.x = abs(p.x);
    float l = length(p.xy);
    p.xy = mat2(-c.x, c.y, 
              c.y, c.x)*p.xy;
    p.xy = vec2((p.y>0.0 || p.x>0.0)?p.x:l*sign(-c.x),
                (p.x>0.0)?p.y:l );
    p.xy = vec2(p.x,abs(p.y-r))-vec2(le,0.0);
    
    vec2 q = vec2(length(max(p.xy,0.0)) + min(0.0,max(p.x,p.y)),p.z);
    vec2 d = abs(q) - w;
    return min(max(d.x,d.y),0.0) + length(max(d,0.0));
}

float randomSDF(vec3 p, float seed) {
    const float size = .3;
    const int SDFcount = 4;
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
    if (seed == 3) { return sdfHorseshoe(p, vec2(cos(1.3),sin(1.3)), 0.35, 0.3, vec2(0.1,0.08) ); }
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

//smooth minimum and is used merging effect between SDF's
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
    SurfaceInfo result;
    if (k == 0.) {
        result.distance = min(a.distance, b.distance);
        result.color = b.color;
        result.reflectivity = b.reflectivity;
        if (a.distance == result.distance) {
            result.color = a.color;
            result.reflectivity = a.reflectivity;
        }
        return result;
    }
    float h = clamp(0.5 + 0.5 * (b.distance - a.distance) / k, 0.0, 1.0);   
    result.distance = mix(b.distance, a.distance, h) - k * h * (1.0 - h);
    result.color = mix(b.color, a.color, h);    //mix is linear interpolation
    result.reflectivity = mix(b.reflectivity, a.reflectivity, h);
    return result;
}

//linearly interpolating between two SDF's
SurfaceInfo mixSurfaces(SurfaceInfo a, SurfaceInfo b, float t) {
    SurfaceInfo result;
    result.distance = mix(a.distance, b.distance, t);
    result.color = mix(a.color, b.color, t);
    result.reflectivity = mix(a.reflectivity, b.reflectivity, t);
    return result;
}

//colorspace conversion 
vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

//defines the scene ie calculates closest surface info, p is the position of the ray
SurfaceInfo map(vec3 p, int bouncedTimes) {
    float smoothing = 0.1; 
    float size = max(arrayX, arrayY) / 4.;

    // Initialize the result with the plane
    SurfaceInfo bgCube;
    vec3 cubePosition = vec3(0., 0., 1.2);
    vec3 cubeSize = vec3(5., 5., .5);
    bgCube.distance = sdfCube(p - cubePosition, cubeSize);
    bgCube.color = vec3(0.0314, 0.0314, 0.0314);
    bgCube.reflectivity = .1;

    // Add mouse sphere 
    SurfaceInfo mouseSphere;
    vec3 mouseSpherePosition = vec3(-mouseX, mouseY, 0) * 3.;
    mouseSphere.distance = sdfSphere((p + mouseSpherePosition) * size, .3) / size;
    mouseSphere.color = vec3(1);
    mouseSphere.reflectivity = 1.;
    
    // Smoosh Cube and Sphere
    SurfaceInfo result = bgCube;

    int k = 0;  //char array index
    //const int randomnessPasses = 3;
    int maxI = int(arrayX);
    int maxJ = int(arrayY);
    int i = 0;
    int j = 0;

    //defining ideal SSBB(Screen Space Bounding Boxes) size manually
    float boxSize = 0;

    switch(int(arrayX)) {
        case 4:
        boxSize = 0.8;
        break;
        case 8:
        boxSize = 0.4;
        break;
        case 10:
        boxSize = 0.3;
        break;
        default:
        boxSize = 0.8;
    }

    //algorytmically defining the grid of SDF's
    for (int i = 0; i < maxI; i++) {
        for (int j = 0; j < maxJ; j++) {
            SurfaceInfo newShape;
            newShape.reflectivity = .3;
            float x = (float(j) / (maxJ - 1)) * 2. - 1.; //position calculations
            float y = (float(i) / (maxI - 1)) * 2. - 1.; 
            float posFix = .7;  //magic number
            vec3 position = vec3(vec2(x * sqrt(arrayX * posFix), -y * sqrt(arrayY * posFix)), 0.);

            //generating SSBB(Screen Space Bounding Boxes) to avoid most intersection tests the primary bottleneck to performance, improves perfrormance by ~4-5x
            if((abs(p.x - position.x) <= boxSize) && (abs(p.y - position.y) <= boxSize)) {
                int c = int(chars[k]);
                float r = random(vec2(c));      //seeding random with encoded char array so pairs will be the same
                
                vec3 q = p;     //rotation
                q -= position;
                q *= rotationMatrix(sampleHemisphereCosine(vec3(1.0, 0.0, 0.0), vec2(r)), time * (r * 2. + .5));    
                
                SurfaceInfo obscured;
                float t = fract(chars[k]) * 2.; //extracting t from the encoded char array(, the interpolation factor)

                //obfuscation calculation
                if (t != 0.) {
                    newShape.distance = randomSDF(q * size , r) / size;
                    vec3 newColor = hsv2rgb(vec3(float(c) / 100., .8, .8));
                    newShape.color = newColor; 
                }
                
                if (t != 1.) {
                    obscured.distance = sdfSphere(q * size, .5) / size;
                    obscured.color = vec3(.9); 
                    obscured.reflectivity = 0.; 
                }

                result = smoothMinSurface(result, mixSurfaces(obscured, newShape, t), smoothing);
            }
            k++;
        }
    }

    result = smoothMinSurface(result, mouseSphere, .5);
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
    vec2 uv = fragCord;     //pixel coordinate conventionally called the uv

    //interlacing looks asthetically pleasing and saves perf as a bonus
    const int scale = 200;
    bool vertical = fract(uv.y * float(scale)) > .5;
    bool horizontal = fract(uv.x * float(scale)) > .5;
    if (vertical) {
        color = vec4(vec3(.1) * abs(sin(time/10)), 1.);
        return;
    }

    vec3 ro = vec3(0.0, 0.0, -3.0); //ray origin
    vec3 rd = normalize(vec3(uv, 1.0)); //ray direction
    vec3 accumulatedColor = vec3(0.0);
    float attenuation = 1.0;

    int i;

    const int bounceTimes = 3;
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
