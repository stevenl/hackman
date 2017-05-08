package Hackman::Player;

use Moose;
use namespace::autoclean;

has id => (
    is       => 'ro',
    isa      => 'Int',
    required => 1,
);

# has name => (
#     is       => 'ro',
#     isa      => 'Str',
#     required => 1,
# );

has point => (
    is       => 'ro',
    isa      => 'Hackman::Point',
    required => 1,
);

# --------------------------------

has snippets => (
    is      => 'ro',
    isa     => 'Int',
    writer  => '_set_snippets',
    default => 0,
);

has has_weapon => (
    is      => 'ro',
    isa     => 'Bool',
    writer  => '_set_has_weapon',
    default => '',
);

has is_paralyzed => (
    is      => 'ro',
    isa     => 'Bool',
    writer  => '_set_is_paralyzed',
    default => '',
);

sub update {
    my ($self, %arg) = @_;

    for my $key (keys %arg) {
        my $val = $arg{$key};
        $self->_set_snippets($val)     if $key eq 'snippets';
        $self->_set_has_weapon($val)   if $key eq 'has_weapon';
        $self->_set_is_paralyzed($val) if $key eq 'is_paralyzed';
    }
    return;
}

__PACKAGE__->meta->make_immutable;
1;
