
uniform mat4 u_MVP;
uniform mat4 u_MVMatrix;
uniform mat4 u_Model;
uniform vec3 u_LightPos;
uniform vec4 u_Color;
uniform mat4 u_Orientations[60];

attribute vec4 a_Position;
attribute vec3 a_Normal;
attribute vec4 a_InstanceData;

varying vec4 v_Color;

void main()
{
   // unpack a_InstanceData
   float orientationAsFloat = a_InstanceData.w;
   vec4 location = vec4( a_InstanceData.xyz, 1.0 );

   int orientation = max( 0, min( 59, int(orientationAsFloat) ) );
   vec4 oriented = ( u_Orientations[ orientation ] * a_Position );
   vec4 normal = ( u_Orientations[ orientation ] * vec4( a_Normal, 0.0 ) );
   vec4 pos = oriented + location;
   gl_Position = u_MVP * pos;

   // original lighting, using a point source
   vec3 modelViewVertex = vec3(u_MVMatrix * pos);
   vec3 modelViewNormal = vec3( u_MVMatrix * vec4( a_Normal, 0.0 ) );
   float distance = length( u_LightPos - modelViewVertex );
   vec3 lightVector = normalize( u_LightPos - modelViewVertex );
   float diffuse = max(dot(modelViewNormal, lightVector), 0.5 );
   diffuse = diffuse * (1.0 / (1.0 + (0.0001 * distance * distance)));
   v_Color = u_Color * diffuse;
}