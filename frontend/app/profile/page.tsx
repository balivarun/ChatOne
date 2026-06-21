'use client';
import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { Camera, ArrowLeft, Save } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { userApi } from '@/lib/api';
import AuthProvider from '@/components/providers/AuthProvider';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import UserAvatar from '@/components/common/UserAvatar';
import { useRouter } from 'next/navigation';
import toast from 'react-hot-toast';
import Image from 'next/image';

function ProfilePageInner() {
  const { user, updateUser } = useAuthStore();
  const router = useRouter();
  const [displayName, setDisplayName] = useState(user?.displayName || '');
  const [bio, setBio] = useState(user?.bio || '');
  const [saving, setSaving] = useState(false);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [avatarFile, setAvatarFile] = useState<File | null>(null);

  const onDrop = useCallback((files: File[]) => {
    const file = files[0];
    if (!file) return;
    setAvatarFile(file);
    setAvatarPreview(URL.createObjectURL(file));
  }, []);

  const { getInputProps, open } = useDropzone({
    onDrop,
    accept: { 'image/*': [] },
    maxFiles: 1,
    noClick: true,
    noKeyboard: true,
  });

  const handleSave = async () => {
    if (!displayName.trim()) {
      toast.error('Display name is required');
      return;
    }
    setSaving(true);
    try {
      if (avatarFile) {
        const { data } = await userApi.updateAvatar(avatarFile);
        updateUser({ avatarUrl: (data.data || data).avatarUrl });
      }
      const { data } = await userApi.updateProfile({
        displayName: displayName.trim(),
        bio: bio.trim() || undefined,
      });
      updateUser(data.data || data);
      toast.success('Profile updated successfully');
    } catch {
      toast.error('Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  if (!user) return null;

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-lg mx-auto py-8 px-4">
        <div className="flex items-center gap-3 mb-8">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => router.back()}
          >
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <h1 className="text-2xl font-bold">Profile</h1>
        </div>

        <div className="bg-white dark:bg-sidebar rounded-2xl shadow-sm p-6 space-y-6">
          {/* Avatar */}
          <div className="flex flex-col items-center gap-3">
            <input {...getInputProps()} />
            <div className="relative">
              {avatarPreview ? (
                <div className="w-24 h-24 rounded-full overflow-hidden relative">
                  <Image
                    src={avatarPreview}
                    alt="Preview"
                    fill
                    className="object-cover"
                  />
                </div>
              ) : (
                <UserAvatar user={user} size="lg" />
              )}
              <button
                type="button"
                onClick={open}
                className="absolute bottom-0 right-0 bg-accent text-white rounded-full p-1.5 shadow-md hover:bg-accent/90 transition-colors"
              >
                <Camera className="w-4 h-4" />
              </button>
            </div>
            <p className="text-sm text-gray-400">Click camera icon to change photo</p>
          </div>

          {/* Form */}
          <div className="space-y-4">
            <div className="space-y-1">
              <Label htmlFor="displayName">Display Name</Label>
              <Input
                id="displayName"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                maxLength={50}
                placeholder="Your display name"
              />
            </div>

            <div className="space-y-1">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                value={user.email}
                disabled
                className="bg-gray-50 dark:bg-gray-800 text-gray-500"
              />
              <p className="text-xs text-gray-400">Email cannot be changed</p>
            </div>

            <div className="space-y-1">
              <Label htmlFor="bio">Bio</Label>
              <textarea
                id="bio"
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                maxLength={200}
                rows={3}
                placeholder="Tell something about yourself"
                className="w-full rounded-md border border-gray-200 dark:border-gray-700 bg-background px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-accent"
              />
              <p className="text-xs text-gray-400 text-right">{bio.length}/200</p>
            </div>
          </div>

          <Button
            onClick={handleSave}
            disabled={saving || !displayName.trim()}
            className="w-full"
          >
            {saving ? (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
            ) : (
              <Save className="h-4 w-4 mr-2" />
            )}
            Save Changes
          </Button>
        </div>

        {/* Account info */}
        <div className="bg-white dark:bg-sidebar rounded-2xl shadow-sm p-6 mt-4 space-y-3">
          <h2 className="font-semibold">Account Info</h2>
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">Member since</span>
            <span>{new Date(user.createdAt).toLocaleDateString()}</span>
          </div>
          {user.lastSeen && (
            <div className="flex justify-between text-sm">
              <span className="text-gray-500">Last seen</span>
              <span>{new Date(user.lastSeen).toLocaleString()}</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function ProfilePage() {
  return (
    <AuthProvider>
      <ProfilePageInner />
    </AuthProvider>
  );
}
