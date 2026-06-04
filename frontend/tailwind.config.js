/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#090b0f',
        panel: '#11151d',
        line: '#272d38',
        signal: '#f2c94c',
        ember: '#ec6f44'
      },
      boxShadow: {
        glow: '0 0 42px rgba(242, 201, 76, 0.16)'
      }
    }
  },
  plugins: []
};
