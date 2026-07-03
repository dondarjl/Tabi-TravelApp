import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Alert, KeyboardAvoidingView, Platform, SafeAreaView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { supabase } from '../services/supabase';

export default function LoginScreen() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [isRegister, setIsRegister] = useState(false);

  async function handleAuth() {
    if (!email || !password) {
      Alert.alert('Faltan datos', 'Introduce email y contraseña');
      return;
    }
    setLoading(true);
    try {
      if (isRegister) {
        const { error } = await supabase.auth.signUp({ email, password });
        if (error) throw error;
        Alert.alert('¡Cuenta creada!', 'Revisa tu email para confirmar la cuenta');
      } else {
        const { error } = await supabase.auth.signInWithPassword({ email, password });
        if (error) throw error;
        router.replace('/(tabs)/' as any);
      }
    } catch (e: any) {
      Alert.alert('Error', e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <SafeAreaView style={styles.container}>
        <View style={styles.body}>
          <Text style={styles.logo}>tabi</Text>
          <Text style={styles.tagline}>Viajes reales, de gente real.</Text>

          <View style={styles.form}>
            <TextInput
              style={styles.input}
              placeholder="Email"
              placeholderTextColor="#bbb"
              value={email}
              onChangeText={setEmail}
              keyboardType="email-address"
              autoCapitalize="none"
            />
            <TextInput
              style={styles.input}
              placeholder="Contraseña"
              placeholderTextColor="#bbb"
              value={password}
              onChangeText={setPassword}
              secureTextEntry
            />
            <TouchableOpacity style={styles.btn} onPress={handleAuth} disabled={loading}>
              <Text style={styles.btnText}>{loading ? '...' : isRegister ? 'Crear cuenta' : 'Entrar'}</Text>
            </TouchableOpacity>
          </View>

          <TouchableOpacity onPress={() => setIsRegister(!isRegister)}>
            <Text style={styles.toggle}>
              {isRegister ? '¿Ya tienes cuenta? Inicia sesión' : '¿No tienes cuenta? Regístrate'}
            </Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F2ED' },
  body: { flex: 1, padding: 32, justifyContent: 'center', gap: 24 },
  logo: { fontSize: 48, fontWeight: '800', color: '#1a1a1a', letterSpacing: -1 },
  tagline: { fontSize: 16, color: '#888', marginTop: -16 },
  form: { gap: 12 },
  input: { backgroundColor: '#fff', borderRadius: 12, padding: 14, fontSize: 15, color: '#1a1a1a', borderWidth: 0.5, borderColor: '#EAE6E0' },
  btn: { backgroundColor: '#D95F2B', borderRadius: 12, padding: 16, alignItems: 'center', marginTop: 4 },
  btnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  toggle: { textAlign: 'center', fontSize: 13, color: '#888' },
});