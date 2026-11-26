# React 19 & Next.js 15 App Router Technical Reference

## 1. React 19 Core Primitives

### A. The `use()` Hook
Read Promises or Context conditionally and inside loops:

```tsx
import { use, Suspense } from 'react';

function UserProfile({ userPromise }: { userPromise: Promise<User> }) {
  // Unwraps promise directly without useEffect or useState
  const user = use(userPromise);
  return <div>Welcome, {user.name}</div>;
}

export default function Page() {
  const userPromise = fetchUser();
  return (
    <Suspense fallback={<div>Loading user profile...</div>}>
      <UserProfile userPromise={userPromise} />
    </Suspense>
  );
}
```

### B. Form Handling with `useActionState` & Server Actions
```tsx
'use client';
import { useActionState } from 'react';
import { updateUsername } from '@/actions/user';

export function EditProfileForm() {
  const [state, formAction, isPending] = useActionState(updateUsername, { error: null });

  return (
    <form action={formAction}>
      <input name="username" required disabled={isPending} />
      <button type="submit" disabled={isPending}>
        {isPending ? 'Saving...' : 'Update'}
      </button>
      {state?.error && <p className="text-red-500">{state.error}</p>}
    </form>
  );
}
```

### C. Optimistic UI with `useOptimistic`
```tsx
'use client';
import { useOptimistic } from 'react';

export function LikeButton({ currentLikes, onLikeAction }: { currentLikes: number, onLikeAction: () => Promise<void> }) {
  const [optimisticLikes, setOptimisticLikes] = useOptimistic(
    currentLikes,
    (state, delta: number) => state + delta
  );

  async function handleLike() {
    setOptimisticLikes(1);
    await onLikeAction();
  }

  return (
    <button onClick={handleLike}>
      ❤️ {optimisticLikes}
    </button>
  );
}
```

---

## 2. Next.js 15 App Router Architecture

### A. Parallel Routes (`@slot`) & Intercepting Routes (`(.)`)
Modal routing without URL loss:
```
app/
├── feed/
│   ├── @modal/
│   │   ├── (.)photo/[id]/page.tsx  <-- Intercepts photo click and opens modal
│   │   └── default.tsx
│   ├── layout.tsx                  <-- Renders {children} and {modal}
│   └── page.tsx
└── photo/
    └── [id]/page.tsx               <-- Hard reload full-page view
```

### B. Route Handler Streaming (`GET /api/stream`)
```typescript
import { NextRequest } from 'next/server';

export async function GET(req: NextRequest) {
  const encoder = new TextEncoder();
  const stream = new ReadableStream({
    async start(controller) {
      for (let i = 0; i < 5; i++) {
        controller.enqueue(encoder.encode(`data: Chunk ${i}\n\n`));
        await new Promise((r) => setTimeout(r, 200));
      }
      controller.close();
    },
  });

  return new Response(stream, {
    headers: {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache, no-transform',
      'Connection': 'keep-alive',
    },
  });
}
```
