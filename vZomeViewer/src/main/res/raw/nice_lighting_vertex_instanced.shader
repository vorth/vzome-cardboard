
uniform mat4 u_Orientations[60];
//uniform mat4 viewInverse;
uniform mat4 worldViewProjection;
uniform mat4 worldInverseTranspose;
uniform vec4 u_Color;
uniform vec3 lightWorldPos;

attribute vec4 a_Position;
attribute vec3 a_Normal;
attribute vec4 a_InstanceData;

varying vec3 v_normal;
varying vec3 v_surfaceToLight;
varying vec3 v_surfaceToView;
varying vec4 v_color;

void main()
{
    // unpack a_InstanceData
    float orientationAsFloat = a_InstanceData.w;
    vec4 location = vec4( a_InstanceData.xyz, 0.0 );

    int orientation = max( 0, min( 59, int(orientationAsFloat) ) );
    vec4 oriented = u_Orientations[ orientation ] * a_Position;
    vec4 orientedNormal = u_Orientations[ orientation ] * vec4( a_Normal, 0.0 );
    vec4 wp = oriented + location;
    gl_Position = worldViewProjection * wp;

    v_normal = (worldInverseTranspose * orientedNormal).xyz;
    v_color = u_Color;
    v_surfaceToLight = lightWorldPos - wp.xyz;
    vec4 colSelector = vec4( 0, 0, 0, 1 );
//    vec4 col4 = viewInverse * colSelector;
    v_surfaceToView = colSelector.xyz - wp.xyz;
}
