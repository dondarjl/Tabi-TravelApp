import { Stack, useRouter } from 'expo-router';
import { useEffect } from 'react';
import { supabase } from '../services/supabase';

console.log('URL:', process.env.EXPO_PUBLIC_SUPABASE_URL);
console.log('KEY:', process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY);

export default function RootLayout() {
  const router = useRouter();

  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      if (session) {
        router.replace('/(tabs)/' as any);
      } else {
        router.replace('/login' as any);
      }
    });

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      if (session) {
        router.replace('/(tabs)/' as any);
      } else {
        router.replace('/login' as any);
      }
    });

    return () => subscription.unsubscribe();
  }, []);

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="(tabs)" />
      <Stack.Screen name="login" />
      <Stack.Screen name="post" />
      <Stack.Screen name="trip/[id]" />
    </Stack>
  );
}