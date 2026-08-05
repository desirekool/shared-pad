import { useMutation } from "@tanstack/react-query";
import * as api from "../auth";

export function useLogin() {
  return useMutation({
    mutationFn: (vars: { username: string; password: string }) =>
      api.login(vars.username, vars.password),
  });
}

export function useRegister() {
  return useMutation({
    mutationFn: (vars: { username: string; email: string; password: string }) =>
      api.register(vars.username, vars.email, vars.password),
  });
}
