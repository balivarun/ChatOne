'use client';
import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { X, Upload, Search } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import UserAvatar from '@/components/common/UserAvatar';
import { userApi, groupApi, fileApi } from '@/lib/api';
import { User } from '@/types';
import { getInitials } from '@/lib/utils';
import toast from 'react-hot-toast';
import { useRouter } from 'next/navigation';
import Image from 'next/image';

interface Props {
  open: boolean;
  onClose: () => void;
}

export default function CreateGroupModal({ open, onClose }: Props) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<User[]>([]);
  const [selectedMembers, setSelectedMembers] = useState<User[]>([]);
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [searching, setSearching] = useState(false);
  const [creating, setCreating] = useState(false);
  const router = useRouter();

  const onAvatarDrop = useCallback((files: File[]) => {
    const file = files[0];
    if (!file) return;
    setAvatarFile(file);
    const url = URL.createObjectURL(file);
    setAvatarPreview(url);
  }, []);

  const { getInputProps: getAvatarInputProps, open: openAvatarPicker } = useDropzone({
    onDrop: onAvatarDrop,
    accept: { 'image/*': [] },
    maxFiles: 1,
    noClick: true,
    noKeyboard: true,
  });

  const handleSearch = async (q: string) => {
    setSearchQuery(q);
    if (!q.trim()) {
      setSearchResults([]);
      return;
    }
    setSearching(true);
    try {
      const { data } = await userApi.searchUsers(q);
      const users: User[] = data.data?.content || data.content || data.data || [];
      setSearchResults(users.filter((u) => !selectedMembers.find((m) => m.id === u.id)));
    } catch {
      toast.error('Search failed');
    } finally {
      setSearching(false);
    }
  };

  const addMember = (user: User) => {
    setSelectedMembers((prev) => [...prev, user]);
    setSearchResults((prev) => prev.filter((u) => u.id !== user.id));
    setSearchQuery('');
  };

  const removeMember = (userId: string) => {
    setSelectedMembers((prev) => prev.filter((u) => u.id !== userId));
  };

  const handleCreate = async () => {
    if (!name.trim()) {
      toast.error('Group name is required');
      return;
    }
    if (selectedMembers.length === 0) {
      toast.error('Add at least one member');
      return;
    }
    setCreating(true);
    try {
      const { data } = await groupApi.create({
        name: name.trim(),
        description: description.trim() || undefined,
        memberIds: selectedMembers.map((m) => m.id),
      });
      const group = data.data || data;
      if (avatarFile) {
        await groupApi.updateAvatar(group.id, avatarFile).catch(() => {});
      }
      toast.success('Group created!');
      onClose();
      router.push(`/group/${group.id}`);
    } catch {
      toast.error('Failed to create group');
    } finally {
      setCreating(false);
    }
  };

  const handleClose = () => {
    setName('');
    setDescription('');
    setSearchQuery('');
    setSearchResults([]);
    setSelectedMembers([]);
    setAvatarFile(null);
    setAvatarPreview(null);
    onClose();
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <DialogContent className="max-w-md max-h-[85vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>Create New Group</DialogTitle>
        </DialogHeader>

        <div className="flex-1 overflow-y-auto space-y-4">
          {/* Avatar */}
          <div className="flex flex-col items-center gap-2">
            <input {...getAvatarInputProps()} />
            <button
              type="button"
              onClick={openAvatarPicker}
              className="w-20 h-20 rounded-full overflow-hidden bg-accent/20 flex items-center justify-center hover:bg-accent/30 transition-colors relative"
            >
              {avatarPreview ? (
                <Image
                  src={avatarPreview}
                  alt="Group avatar"
                  fill
                  className="object-cover"
                />
              ) : (
                <Upload className="w-6 h-6 text-accent" />
              )}
            </button>
            <p className="text-xs text-gray-400">Click to upload group photo</p>
          </div>

          {/* Name */}
          <div className="space-y-1">
            <Label htmlFor="group-name">Group Name *</Label>
            <Input
              id="group-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Enter group name"
              maxLength={50}
            />
          </div>

          {/* Description */}
          <div className="space-y-1">
            <Label htmlFor="group-desc">Description</Label>
            <Input
              id="group-desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Group description (optional)"
              maxLength={200}
            />
          </div>

          {/* Member search */}
          <div className="space-y-2">
            <Label>Add Members</Label>
            {selectedMembers.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {selectedMembers.map((member) => (
                  <div
                    key={member.id}
                    className="flex items-center gap-1 bg-accent/10 text-accent rounded-full px-2 py-1"
                  >
                    <span className="text-xs font-medium">{member.displayName}</span>
                    <button
                      onClick={() => removeMember(member.id)}
                      className="hover:text-red-500"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </div>
                ))}
              </div>
            )}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                value={searchQuery}
                onChange={(e) => handleSearch(e.target.value)}
                placeholder="Search users to add"
                className="pl-9"
              />
            </div>
            {searchResults.length > 0 && (
              <div className="border border-gray-200 dark:border-gray-700 rounded-lg max-h-40 overflow-y-auto">
                {searchResults.map((user) => (
                  <div
                    key={user.id}
                    onClick={() => addMember(user)}
                    className="flex items-center gap-3 px-3 py-2 hover:bg-gray-50 dark:hover:bg-gray-800 cursor-pointer transition-colors"
                  >
                    <UserAvatar user={user} size="sm" />
                    <div>
                      <p className="text-sm font-medium">{user.displayName}</p>
                      <p className="text-xs text-gray-400">{user.email}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={creating}>
            Cancel
          </Button>
          <Button onClick={handleCreate} disabled={creating || !name.trim()}>
            {creating ? (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
            ) : null}
            Create Group
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
