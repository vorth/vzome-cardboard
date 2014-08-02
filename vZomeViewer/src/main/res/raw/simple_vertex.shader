attribute vec4 a_Position;
uniform mat4 u_MVP;
uniform vec4 u_Color;
varying vec4 v_Color;

void main() {
  v_Color = u_Color;
  gl_Position = u_MVP * a_Position;
}