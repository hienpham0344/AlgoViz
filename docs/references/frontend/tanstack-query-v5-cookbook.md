# TanStack Query v5 (React Query) Production Cookbook

## 1. Query Key Factory Pattern
Eliminates magic strings and guarantees deterministic cache invalidation:

```typescript
export const userKeys = {
  all: ['users'] as const,
  lists: () => [...userKeys.all, 'list'] as const,
  list: (filters: string) => [...userKeys.lists(), { filters }] as const,
  details: () => [...userKeys.all, 'detail'] as const,
  detail: (id: string) => [...userKeys.details(), id] as const,
};
```

---

## 2. Optimistic Mutation Pattern
Update the UI immediately with rollback safety on network failure:

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { userKeys } from './userKeys';

export function useUpdateUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateUserApi,
    // Step 1: When mutate is called:
    onMutate: async (newUser) => {
      // Cancel outgoing refetches so they don't overwrite optimistic update
      await queryClient.cancelQueries({ queryKey: userKeys.detail(newUser.id) });

      // Snapshot previous value
      const previousUser = queryClient.getQueryData(userKeys.detail(newUser.id));

      // Optimistically update cache
      queryClient.setQueryData(userKeys.detail(newUser.id), newUser);

      // Return context object with snapshotted value
      return { previousUser };
    },
    // Step 2: If mutation fails, rollback to snapshot:
    onError: (err, newUser, context) => {
      if (context?.previousUser) {
        queryClient.setQueryData(userKeys.detail(newUser.id), context.previousUser);
      }
    },
    // Step 3: Always refetch after error or success:
    onSettled: (data, error, variables) => {
      queryClient.invalidateQueries({ queryKey: userKeys.detail(variables.id) });
    },
  });
}
```

---

## 3. Server-Side Prefetching & Dehydration (Next.js)
```tsx
import { dehydrate, HydrationBoundary, QueryClient } from '@tanstack/react-query';
import { userKeys } from '@/queries/userKeys';
import { fetchUsers } from '@/api/users';
import UsersList from './UsersList';

export default async function Page() {
  const queryClient = new QueryClient();

  await queryClient.prefetchQuery({
    queryKey: userKeys.lists(),
    queryFn: fetchUsers,
  });

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <UsersList />
    </HydrationBoundary>
  );
}
```
