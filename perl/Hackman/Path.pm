package Hackman::Path;

use Moose;
use Moose::Util::TypeConstraints;
use namespace::autoclean;

use constant DIRECTIONS => qw(up down left right);
enum 'Hackman::Move', [DIRECTIONS];

has start => (
    is       => 'ro',
    isa      => 'Hackman::Point',
    required => 1,
);

has end => (
    is      => 'ro',
    isa     => 'Hackman::Point',
    builder => '_build_end',
    lazy    => 1,
);

has moves => (
    is      => 'bare',
    isa     => 'ArrayRef[Hackman::Move]',
    traits  => ['Array'],
    handles => {
        moves      => 'elements',
        nr_moves   => 'count',
        first_move => ['get', 0],
    },
    default => sub { [] },
);

sub _build_end {
    my ($self) = @_;

    my $point = $self->start;
    for my $direction ($self->moves) {
        $point = $point->move($direction);
    }
    return $point;
}

sub add_move {
    my ($self, $direction) = @_;

    my $class = $self->meta->name;
    return $class->new(
        start => $self->start,
        end   => $self->end->move($direction),
        moves => [$self->moves, $direction],
    );
}

__PACKAGE__->meta->make_immutable;
1;
