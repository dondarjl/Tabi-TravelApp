import { useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from 'react-native';
import { supabase } from '../services/supabase';

export default function EditProfileScreen() {
  const router = useRouter();
  const [userId, setUserId] = useState<string | null>(null);
  const [username, setUsername] = useState('');
  const [bio, setBio] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    async function loadUser() {
      const { data: { user: authUser } } = await supabase.auth.getUser();
      if (!authUser) { router.replace('/login' as any); return; }

      setUserId(authUser.id);

      const { data } = await supabase
        .from('users')
        .select('username, bio')
        .eq('id', authUser.id)
        .single();

      if (data) {
        setUsername(data.username || '');
        setBio(data.bio || '');
      }

      setLoading(false);
    }

    loadUser();
  }, []);

  async function handleSave() {
    if (!userId) return;
    if (!username.trim()) {
      Alert.alert('Campo requerido', 'El nombre de usuario no puede estar vacío.');
      return;
    }

    setSaving(true);

    const { error } = await supabase
      .from('users')
      .update({ username: username.trim(), bio: bio.trim() })
      .eq('id', userId);

    setSaving(false);

    if (error) {
      Alert.alert('Error', 'No se pudo guardar. Inténtalo de nuevo.');
    } else {
      router.back();
    }
  }



  if (loading) return (
    <SafeAreaView style={styles.container}>
      <ActivityIndicator style={{ flex: 1 }} color="#D95F2B" />
    </SafeAreaView>
  );

  return (
    <SafeAreaView style={styles.container}>
      {/* Header */}
      <View style={styles.topBar}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>←</Text>
        </TouchableOpacity>
        <Text style={styles.topBarTitle}>Editar perfil</Text>
        <TouchableOpacity
          style={[styles.saveBtn, saving && styles.saveBtnDisabled]}
          onPress={handleSave}
          disabled={saving}
        >
          {saving
            ? <ActivityIndicator size="small" color="#fff" />
            : <Text style={styles.saveBtnText}>Guardar</Text>
          }
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.body}>
        {/* Avatar placeholder */}
        <View style={styles.avatarSection}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>
              {username ? username.slice(0, 2).toUpperCase() : '??'}
            </Text>
          </View>
          <Text style={styles.avatarHint}>Foto de perfil · próximamente</Text>
        </View>

        {/* Campos */}
        <View style={styles.fields}>
          <View style={styles.field}>
            <Text style={styles.label}>Nombre de usuario</Text>
            <TextInput
              style={styles.input}
              value={username}
              onChangeText={setUsername}
              placeholder="tu_usuario"
              placeholderTextColor="#ccc"
              autoCapitalize="none"
              autoCorrect={false}
              maxLength={30}
            />
            <Text style={styles.hint}>{username.length}/30</Text>
          </View>

          <View style={styles.field}>
            <Text style={styles.label}>Bio</Text>
            <TextInput
              style={[styles.input, styles.inputMultiline]}
              value={bio}
              onChangeText={setBio}
              placeholder="Cuéntanos algo sobre ti..."
              placeholderTextColor="#ccc"
              multiline
              numberOfLines={3}
              maxLength={150}
            />
            <Text style={styles.hint}>{bio.length}/150</Text>
          </View>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F5F2ED' },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 0.5,
    borderBottomColor: '#F0EDE8',
  },
  backBtn: {
    width: 32, height: 32, borderRadius: 16,
    backgroundColor: '#F5F2ED',
    alignItems: 'center', justifyContent: 'center',
  },
  backText: { fontSize: 16, color: '#1a1a1a' },
  topBarTitle: { fontSize: 15, fontWeight: '700', color: '#1a1a1a' },
  saveBtn: {
    backgroundColor: '#D95F2B',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 6,
    minWidth: 72,
    alignItems: 'center',
  },
  saveBtnDisabled: { opacity: 0.6 },
  saveBtnText: { color: '#fff', fontSize: 13, fontWeight: '700' },
  body: { padding: 20, gap: 24 },
  avatarSection: { alignItems: 'center', gap: 8, paddingVertical: 8 },
  avatar: {
    width: 72, height: 72, borderRadius: 36,
    backgroundColor: '#FAEEDA',
    alignItems: 'center', justifyContent: 'center',
  },
  avatarText: { fontSize: 22, fontWeight: '800', color: '#633806' },
  avatarHint: { fontSize: 11, color: '#bbb' },
  fields: { gap: 16 },
  field: { gap: 6 },
  label: { fontSize: 12, fontWeight: '700', color: '#888', textTransform: 'uppercase', letterSpacing: 0.5 },
  input: {
    backgroundColor: '#fff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#EAE6E0',
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 15,
    color: '#1a1a1a',
  },
  inputMultiline: { minHeight: 90, textAlignVertical: 'top', paddingTop: 12 },
  hint: { fontSize: 11, color: '#ccc', textAlign: 'right' },

});
