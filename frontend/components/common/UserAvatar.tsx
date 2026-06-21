import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { getInitials } from '@/lib/utils';
import { User } from '@/types';

interface Props {
  user: Pick<User, 'displayName' | 'avatarUrl' | 'isOnline'>;
  size?: 'sm' | 'md' | 'lg';
  showStatus?: boolean;
}

export default function UserAvatar({
  user,
  size = 'md',
  showStatus = false,
}: Props) {
  const sizeClass = {
    sm: 'w-8 h-8 text-xs',
    md: 'w-10 h-10 text-sm',
    lg: 'w-16 h-16 text-xl',
  }[size];

  return (
    <div className="relative inline-block">
      <Avatar className={`${sizeClass} rounded-full overflow-hidden bg-accent/20`}>
        <AvatarImage
          src={user.avatarUrl}
          alt={user.displayName}
          className="object-cover"
        />
        <AvatarFallback className="bg-accent text-white font-semibold">
          {getInitials(user.displayName)}
        </AvatarFallback>
      </Avatar>
      {showStatus && (
        <span
          className={`absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-sidebar ${
            user.isOnline ? 'bg-green-500' : 'bg-gray-400'
          }`}
        />
      )}
    </div>
  );
}
