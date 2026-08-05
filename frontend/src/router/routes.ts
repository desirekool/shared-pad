import {
  createRouter,
  createRootRoute,
  createRoute,
  redirect,
} from "@tanstack/react-router";
import Login from "../pages/Login";
import Register from "../pages/Register";
import DocumentList from "../pages/docs/DocumentList";
import DocumentEditor from "../pages/docs/DocumentEditor";
import Home from "../pages/Home";

function authGuard() {
  const token = localStorage.getItem("token");
  if (!token) {
    throw redirect({ to: "/login" });
  }
}

function redirectIfAuthed() {
  const token = localStorage.getItem("token");
  if (token) {
    throw redirect({ to: "/" });
  }
}

const rootRoute = createRootRoute();

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/login",
  component: Login,
  beforeLoad: redirectIfAuthed,
});

const registerRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/register",
  component: Register,
  beforeLoad: redirectIfAuthed,
});

const homeRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/",
  component: Home,
  beforeLoad: authGuard,
});

const docsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/docs",
  component: DocumentList,
  beforeLoad: authGuard,
});

const docRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/docs/$id",
  component: DocumentEditor,
  beforeLoad: authGuard,
});

const routeTree = rootRoute.addChildren([
  loginRoute,
  registerRoute,
  homeRoute,
  docsRoute,
  docRoute,
]);

const router = createRouter({ routeTree });

declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}

export default router;
