FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:22-alpine
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY --from=build /app/dist-electron ./dist-electron
COPY --from=build /app/package.json ./
RUN npm ci --production
EXPOSE 5173
CMD ["node", "dist-electron/main.js"]
